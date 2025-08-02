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
import java.math.BigDecimal;
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

    public static final List<String> DEFAULT_ROLES = List.of(
            "ADMIN", "RECEPTIONIST", "DOCTOR", "CASHIER", "TECHNICIAN", "PATIENT"
    );
    public record PermissionSeed(String name, String description, String groupName) {}

    public static final List<PermissionSeed> DEFAULT_PERMISSIONS = List.of(
            new PermissionSeed("ACCOUNTS:CREATE", "Tạo tài khoản", "Tài khoản"),
            new PermissionSeed("ACCOUNTS:READ", "Xem tài khoản", "Tài khoản"),
            new PermissionSeed("ACCOUNTS:UPDATE", "Cập nhật tài khoản", "Tài khoản"),
            new PermissionSeed("ACCOUNTS:DELETE", "Xóa tài khoản", "Tài khoản"),

            new PermissionSeed("DEPARTMENTS:READ", "Xem phòng ban", "Phòng ban"),
            new PermissionSeed("DEPARTMENTS:CREATE", "Tạo phòng ban", "Phòng ban"),
            new PermissionSeed("DEPARTMENTS:UPDATE", "Cập nhật phòng ban", "Phòng ban"),
            new PermissionSeed("DEPARTMENTS:DELETE", "Xóa phòng ban", "Phòng ban"),

            new PermissionSeed("INVOICES:READ", "Xem hóa đơn", "Hóa đơn"),
            new PermissionSeed("INVOICES:CREATE", "Tạo hóa đơn", "Hóa đơn"),
            new PermissionSeed("INVOICES:UPDATE", "Cập nhật hóa đơn", "Hóa đơn"),
            new PermissionSeed("INVOICES:DELETE", "Xóa hóa đơn", "Hóa đơn"),

            new PermissionSeed("INVOICE-ITEMS:READ", "Xem mục hóa đơn", "Chi tiết hóa đơn"),
            new PermissionSeed("INVOICE-ITEMS:CREATE", "Thêm mục hóa đơn", "Chi tiết hóa đơn"),
            new PermissionSeed("INVOICE-ITEMS:UPDATE", "Cập nhật mục hóa đơn", "Chi tiết hóa đơn"),
            new PermissionSeed("INVOICE-ITEMS:DELETE", "Xóa mục hóa đơn", "Chi tiết hóa đơn"),

            new PermissionSeed("LEAVE-REQUESTS:READ", "Xem đơn xin nghỉ", "Đơn xin nghỉ"),
            new PermissionSeed("LEAVE-REQUESTS:CREATE", "Tạo đơn xin nghỉ", "Đơn xin nghỉ"),
            new PermissionSeed("LEAVE-REQUESTS:UPDATE", "Cập nhật đơn xin nghỉ", "Đơn xin nghỉ"),
            new PermissionSeed("LEAVE-REQUESTS:DELETE", "Xóa đơn xin nghỉ", "Đơn xin nghỉ"),

            new PermissionSeed("MEDICAL-ORDERS:READ", "Xem chỉ định", "Chỉ định"),
            new PermissionSeed("MEDICAL-ORDERS:CREATE", "Tạo chỉ định", "Chỉ định"),
            new PermissionSeed("MEDICAL-ORDERS:UPDATE", "Cập nhật chỉ định", "Chỉ định"),
            new PermissionSeed("MEDICAL-ORDERS:DELETE", "Xóa chỉ định", "Chỉ định"),

            new PermissionSeed("MEDICAL-RECORDS:READ", "Xem bệnh án", "Bệnh án"),
            new PermissionSeed("MEDICAL-RECORDS:CREATE", "Tạo bệnh án", "Bệnh án"),
            new PermissionSeed("MEDICAL-RECORDS:UPDATE", "Cập nhật bệnh án", "Bệnh án"),
            new PermissionSeed("MEDICAL-RECORDS:DELETE", "Xóa bệnh án", "Bệnh án"),

            new PermissionSeed("MEDICAL-RESULTS:READ", "Xem kết quả xét nghiệm", "Kết quả xét nghiệm"),
            new PermissionSeed("MEDICAL-RESULTS:CREATE", "Thêm kết quả xét nghiệm", "Kết quả xét nghiệm"),
            new PermissionSeed("MEDICAL-RESULTS:UPDATE", "Cập nhật kết quả xét nghiệm", "Kết quả xét nghiệm"),
            new PermissionSeed("MEDICAL-RESULTS:DELETE", "Xóa kết quả xét nghiệm", "Kết quả xét nghiệm"),

            new PermissionSeed("MEDICAL-SERVICES:READ", "Xem dịch vụ y tế", "Dịch vụ y tế"),
            new PermissionSeed("MEDICAL-SERVICES:CREATE", "Tạo dịch vụ y tế", "Dịch vụ y tế"),
            new PermissionSeed("MEDICAL-SERVICES:UPDATE", "Cập nhật dịch vụ y tế", "Dịch vụ y tế"),
            new PermissionSeed("MEDICAL-SERVICES:DELETE", "Xóa dịch vụ y tế", "Dịch vụ y tế"),

            new PermissionSeed("PATIENTS:READ", "Xem bệnh nhân", "Bệnh nhân"),
            new PermissionSeed("PATIENTS:CREATE", "Tạo bệnh nhân", "Bệnh nhân"),
            new PermissionSeed("PATIENTS:UPDATE", "Cập nhật bệnh nhân", "Bệnh nhân"),
            new PermissionSeed("PATIENTS:DELETE", "Xóa bệnh nhân", "Bệnh nhân"),

            new PermissionSeed("PERMISSIONS:READ", "Xem quyền", "Quyền"),
            new PermissionSeed("PERMISSIONS:CREATE", "Tạo quyền", "Quyền"),
            new PermissionSeed("PERMISSIONS:UPDATE", "Cập nhật quyền", "Quyền"),
            new PermissionSeed("PERMISSIONS:DELETE", "Xóa quyền", "Quyền"),

            new PermissionSeed("ROLES:READ", "Xem vai trò", "Vai trò"),
            new PermissionSeed("ROLES:CREATE", "Tạo vai trò", "Vai trò"),
            new PermissionSeed("ROLES:UPDATE", "Cập nhật vai trò", "Vai trò"),
            new PermissionSeed("ROLES:DELETE", "Xóa vai trò", "Vai trò"),

            new PermissionSeed("QUEUE-PATIENTS:READ", "Xem danh sách khám", "Danh sách khám"),
            new PermissionSeed("QUEUE-PATIENTS:CREATE", "Thêm vào danh sách khám", "Danh sách khám"),
            new PermissionSeed("QUEUE-PATIENTS:UPDATE", "Cập nhật danh sách khám", "Danh sách khám"),
            new PermissionSeed("QUEUE-PATIENTS:DELETE", "Xóa khỏi danh sách khám", "Danh sách khám"),

            new PermissionSeed("SETTINGS:READ", "Xem cấu hình hệ thống", "Cài đặt"),
            new PermissionSeed("SETTINGS:CREATE", "Thêm cấu hình hệ thống", "Cài đặt"),
            new PermissionSeed("SETTINGS:UPDATE", "Cập nhật cấu hình hệ thống", "Cài đặt"),
            new PermissionSeed("SETTINGS:DELETE", "Xóa cấu hình hệ thống", "Cài đặt"),

            new PermissionSeed("SHIFTS:READ", "Xem ca trực", "Ca trực"),
            new PermissionSeed("SHIFTS:CREATE", "Tạo ca trực", "Ca trực"),
            new PermissionSeed("SHIFTS:UPDATE", "Cập nhật ca trực", "Ca trực"),
            new PermissionSeed("SHIFTS:DELETE", "Xóa ca trực", "Ca trực"),

            new PermissionSeed("STAFFS:READ", "Xem nhân viên", "Nhân sự"),
            new PermissionSeed("STAFFS:CREATE", "Thêm nhân viên", "Nhân sự"),
            new PermissionSeed("STAFFS:UPDATE", "Cập nhật nhân viên", "Nhân sự"),
            new PermissionSeed("STAFFS:DELETE", "Xóa nhân viên", "Nhân sự"),

            new PermissionSeed("TEMPLATE-FILES:READ", "Xem mẫu biểu", "Mẫu biểu"),
            new PermissionSeed("TEMPLATE-FILES:CREATE", "Tạo mẫu biểu", "Mẫu biểu"),
            new PermissionSeed("TEMPLATE-FILES:UPDATE", "Cập nhật mẫu biểu", "Mẫu biểu"),
            new PermissionSeed("TEMPLATE-FILES:DELETE", "Xóa mẫu biểu", "Mẫu biểu"),

            new PermissionSeed("WORK-SCHEDULES:READ", "Xem lịch làm việc", "Lịch làm việc"),
            new PermissionSeed("WORK-SCHEDULES:CREATE", "Tạo lịch làm việc", "Lịch làm việc"),
            new PermissionSeed("WORK-SCHEDULES:UPDATE", "Cập nhật lịch làm việc", "Lịch làm việc"),
            new PermissionSeed("WORK-SCHEDULES:DELETE", "Xóa lịch làm việc", "Lịch làm việc"),

            new PermissionSeed("SPECIALIZATIONS:READ", "Xem chuyen khoa", "chuyên khoa"),
            new PermissionSeed("SPECIALIZATIONS:CREATE", "Tạo chuyen khoa", "chuyên khoa"),
            new PermissionSeed("SPECIALIZATIONS:UPDATE", "Cập nhật chuyen khoa", "chuyên khoa"),
            new PermissionSeed("SPECIALIZATIONS:DELETE", "Xóa chuyen khoa", "chuyên khoa")
    );


    public void seedDefaultData(Tenant tenant) {
        JdbcTemplate jdbcTemplate = jdbcTemplateFactory.create(tenant.getCode());

        // ====== 1. Seed Roles (batch) ======
        List<String> roles = DEFAULT_ROLES;
        String insertRoleSql = "INSERT INTO roles (name, description, created_at, updated_at) VALUES (?, ?, NOW(), NOW())";
        jdbcTemplate.batchUpdate(insertRoleSql, roles, roles.size(), (ps, role) -> {
            ps.setString(1, role);
            ps.setString(2, role + " role");
        });

        // ====== 2. Seed Permissions (batch) ======
        List<PermissionSeed> permissions = DEFAULT_PERMISSIONS;

        String insertPermissionSql = "INSERT INTO permissions (name, description, group_name, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        jdbcTemplate.batchUpdate(insertPermissionSql, permissions, permissions.size(), (ps, p) -> {
            ps.setString(1, p.name());
            ps.setString(2, p.description());
            ps.setString(3, p.groupName());
        });


        // ====== 3. Seed Role-Permissions (batch) ======
        List<Object[]> rolePermissionParams = new ArrayList<>();
        for (String role : roles) {
            for (PermissionSeed permission : permissions) {
                rolePermissionParams.add(new Object[]{role, permission.name()});
            }
        }
        String insertRolePermissionSql = "INSERT INTO role_permissions (role_name, permission_name) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(insertRolePermissionSql, rolePermissionParams);

        // ====== 4. Insert default setting ======
        jdbcTemplate.update(
                "INSERT INTO settings (" +
                        "id, hospital_name, hospital_phone, hospital_email, hospital_address, " +
                        "bank_account_number, bank_code, pagination_size_list, latest_check_in_minutes, " +
                        "queue_open_time, queue_close_time, min_booking_days_before, min_leave_days_before, " +
                        "monthly_target_revenue, created_at, updated_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                UUID.randomUUID().toString(),
                tenant.getName(),
                tenant.getPhone(),
                tenant.getEmail(),
                "",                    // address
                "",                    // bank account
                "",                    // bank code
                "5,10,20,50",          // pagination list
                15,                    // latest check-in
                LocalTime.of(7, 0),    // queue_open_time
                LocalTime.of(17, 0),   // queue_close_time
                1,                     // min_booking_days_before
                1,                     // min_leave_days_before
                new BigDecimal("10000000") // 👈 10 triệu VND
        );



        // ====== 5. Insert default admin account and role ======
        String password = DataUtil.generateRandomPassword(10);
        String hashedPassword = passwordEncoder.encode(password);

        String accountId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO accounts (id, username, password, is_tenant, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                accountId,
                "admin",
                hashedPassword,
                true
        );

        jdbcTemplate.update(
                "INSERT INTO account_roles (account_id, role_name) VALUES (?, ?)",
                accountId,
                "ADMIN"
        );

        // ====== 6. Insert default templates ======
        insertTemplate("medical_record_template.docx", "MEDICAL_RECORD", true, tenant);
        insertTemplate("invoice_template.docx", "INVOICE", true, tenant);

        // ====== 7. Send credential email ======
        queueEmail(tenant, "Thông tin tài khoản", "admin", password);

        log.info("Default data inserted for tenant: {}", tenant.getCode());
    }

    private void queueEmail(Tenant tenant, String subject, String username, String password) {
        String url = "https://" + tenant.getCode() + "." + domain + "/home";
        String content;

        try {
            ClassPathResource resource = new ClassPathResource("templates/welcome-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template.replace("{{name}}", tenant.getName()).replace("{{url}}", url)
                    .replace("{{username}}", username).replace("{{password}}", password);
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
                log.warn("⚠️ Không tìm thấy file template {}", fileName);
                return;
            }

            String originalFilename = fileName;
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String uuid = UUID.randomUUID().toString();
            String docxFileName = baseName + "_" + uuid + ".docx";
            String pdfFileName = baseName + "_" + uuid + ".pdf";

            // 1. Tạo MultipartFile từ file trong resources
            MultipartFile docxFile = new ByteArrayMultipartFile(
                    inputStream.readAllBytes(),
                    docxFileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );

            // 2. Upload DOCX
            String fileUrl = fileStorageService.storeDocFile(docxFile, "", docxFileName);

            // 3. Convert sang PDF và upload
            byte[] pdfBytes = docxConverterService.convertDocxToPdf(docxFile);
            MultipartFile pdfFile = new ByteArrayMultipartFile(pdfBytes, pdfFileName, "application/pdf");
            String previewUrl = fileStorageService.storeDocFile(pdfFile, "", pdfFileName);

            // 4. Insert vào bảng template_files
            JdbcTemplate jdbcTemplate = jdbcTemplateFactory.create(tenant.getCode());
            jdbcTemplate.update(
                    "INSERT INTO template_files (id, name, type, file_url, preview_url, is_default, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    UUID.randomUUID().toString(),
                    baseName,
                    type,
                    fileUrl,
                    previewUrl,
                    isDefault
            );

            log.info("✅ Inserted template file '{}' for tenant {}", fileName, tenant.getCode());
        } catch (Exception e) {
            log.error("❌ Không thể seed file template {} cho tenant {}: {}", fileName, tenant.getCode(), e.getMessage(), e);
        }
    }
}
