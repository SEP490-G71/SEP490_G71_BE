package vn.edu.fpt.medicaldiagnosis.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.entity.ByteArrayMultipartFile;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.FileStorageService;
import vn.edu.fpt.medicaldiagnosis.service.JdbcTemplateFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultDataSeeder {
    private final JdbcTemplateFactory jdbcTemplateFactory;
    private final PasswordEncoder passwordEncoder;
    private final EmailTaskRepository emailTaskRepository;
    private final FileStorageService fileStorageService;
    private final DocxConverterService docxConverterService;

    @Value("${cloudflare.domain}")
    private String domain;

    public static final List<String> DEFAULT_ROLES = List.of("ADMIN", "RECEPTIONIST", "DOCTOR", "CASHIER", "TECHNICIAN", "PATIENT");
    public record PermissionSeed(String name, String description, String groupName) {}
    public static final List<PermissionSeed> DEFAULT_PERMISSIONS = List.of(
            // List of permissions here (unchanged for brevity)
    );

    public void seedDefaultData(Tenant tenant) {
        JdbcTemplate jdbcTemplate = jdbcTemplateFactory.create(tenant.getCode());

        // 1. Roles
        String insertRoleSql = "INSERT IGNORE INTO roles (name, description, created_at, updated_at) VALUES (?, ?, NOW(), NOW())";
        jdbcTemplate.batchUpdate(insertRoleSql, DEFAULT_ROLES, DEFAULT_ROLES.size(), (ps, role) -> {
            ps.setString(1, role);
            ps.setString(2, role + " role");
        });

        // 2. Permissions
        String insertPermissionSql = "INSERT IGNORE INTO permissions (name, description, group_name, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        jdbcTemplate.batchUpdate(insertPermissionSql, DEFAULT_PERMISSIONS, DEFAULT_PERMISSIONS.size(), (ps, p) -> {
            ps.setString(1, p.name());
            ps.setString(2, p.description());
            ps.setString(3, p.groupName());
        });

        // 3. Role-Permissions
        List<Object[]> rolePermissionParams = new ArrayList<>();
        for (String role : DEFAULT_ROLES) {
            for (PermissionSeed permission : DEFAULT_PERMISSIONS) {
                rolePermissionParams.add(new Object[]{role, permission.name()});
            }
        }
        jdbcTemplate.batchUpdate("INSERT IGNORE INTO role_permissions (role_name, permission_name) VALUES (?, ?)", rolePermissionParams);

        // 4. Settings
        jdbcTemplate.update(
                "INSERT INTO settings (id, hospital_name, hospital_phone, hospital_email, hospital_address, bank_account_number, bank_code, pagination_size_list, latest_check_in_minutes, queue_open_time, queue_close_time, min_booking_days_before, min_leave_days_before, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                UUID.randomUUID().toString(), tenant.getName(), tenant.getPhone(), tenant.getEmail(), "", "", "", "5,10,20,50", 15, LocalTime.of(7, 0), LocalTime.of(17, 0), 1, 1
        );

        // 5. Admin account
        String password = DataUtil.generateRandomPassword(10);
        String hashedPassword = passwordEncoder.encode(password);
        String accountId = UUID.randomUUID().toString();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts WHERE username = ?", Integer.class, "admin");
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO accounts (id, username, password, is_tenant, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())", accountId, "admin", hashedPassword, true);
            jdbcTemplate.update("INSERT INTO account_roles (account_id, role_name) VALUES (?, ?)", accountId, "ADMIN");
            queueEmail(tenant, "Thông tin tài khoản", "admin", password);
        } else {
            log.info("Tài khoản 'admin' đã tồn tại trong tenant {}, bỏ qua tạo mới.", tenant.getCode());
        }

        // 6. Templates
        insertTemplate("medical_record_template.docx", "MEDICAL_RECORD", true, tenant);
        insertTemplate("invoice_template.docx", "INVOICE", true, tenant);

        log.info("Default data inserted for tenant: {}", tenant.getCode());
    }

    private void queueEmail(Tenant tenant, String subject, String username, String password) {
        String url = "https://" + tenant.getCode() + "." + domain + "/home";
        String content;
        try {
            ClassPathResource resource = new ClassPathResource("templates/welcome-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template.replace("{{name}}", tenant.getName()).replace("{{url}}", url).replace("{{username}}", username).replace("{{password}}", password);
        } catch (Exception e) {
            log.info("Không thể load template welcome-email.html: {}", e.getMessage());
            content = String.format("Xin chào %s,\n\nTruy cập hệ thống tại: %s\n\nTrân trọng.", tenant.getName(), url);
        }

        emailTaskRepository.save(EmailTask.builder()
                .id(UUID.randomUUID().toString())
                .emailTo(tenant.getEmail())
                .subject(subject)
                .content(content)
                .status(Status.PENDING)
                .retryCount(0)
                .build());

        log.info("Queued email for tenant {} to {}", tenant.getCode(), tenant.getEmail());
    }

    private void insertTemplate(String fileName, String type, boolean isDefault, Tenant tenant) {
        try (InputStream inputStream = getClass().getResourceAsStream("/default/" + fileName)) {
            if (inputStream == null) {
                log.warn("Không tìm thấy file template {}", fileName);
                return;
            }

            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String uuid = UUID.randomUUID().toString();
            String docxFileName = baseName + "_" + uuid + ".docx";
            String pdfFileName = baseName + "_" + uuid + ".pdf";

            MultipartFile docxFile = new ByteArrayMultipartFile(inputStream.readAllBytes(), docxFileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            String fileUrl = fileStorageService.storeDocFile(docxFile, "", docxFileName);

            byte[] pdfBytes = docxConverterService.convertDocxToPdf(docxFile);
            MultipartFile pdfFile = new ByteArrayMultipartFile(pdfBytes, pdfFileName, "application/pdf");
            String previewUrl = fileStorageService.storeDocFile(pdfFile, "", pdfFileName);

            JdbcTemplate jdbcTemplate = jdbcTemplateFactory.create(tenant.getCode());
            jdbcTemplate.update("INSERT IGNORE INTO template_files (id, name, type, file_url, preview_url, is_default, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    UUID.randomUUID().toString(), baseName, type, fileUrl, previewUrl, isDefault);

            log.info("Inserted template file '{}' for tenant {}", fileName, tenant.getCode());
        } catch (Exception e) {
            log.error("Không thể seed file template {} cho tenant {}: {}", fileName, tenant.getCode(), e.getMessage(), e);
        }
    }
}
