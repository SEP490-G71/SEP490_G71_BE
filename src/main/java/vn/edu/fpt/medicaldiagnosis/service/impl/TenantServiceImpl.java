package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.PurchasePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.TransactionHistoryRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.TenantResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.Action;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.service.TransactionHistoryService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    private final DataSource controlDataSource;
    private final TenantSchemaInitializer schemaInitializer;
    private final DataSourceProvider dataSourceProvider;
    private final DbTaskRepository dbTaskRepository;
    private final EmailTaskRepository emailTaskRepository;
    private final CloudflareTaskRepository cloudflareTaskRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final TransactionHistoryService transactionHistoryService;

    @Value("${cloudflare.domain}")
    private String domain;

    @Value("${database.host}")
    private String host;

    @Value("${database.port}")
    private String port;

    @Value("${spring.datasource.control.username}")
    private String rootUsername;

    @Value("${spring.datasource.control.password}")
    private String rootPassword;

    @Autowired
    public TenantServiceImpl(@Qualifier("controlDataSource") DataSource controlDataSource,
                             @Lazy TenantSchemaInitializer schemaInitializer,
                             @Lazy DataSourceProvider dataSourceProvider,
                             DbTaskRepository dbTaskRepository,
                             EmailTaskRepository emailTaskRepository,
                             CloudflareTaskRepository cloudflareTaskRepository,
                             ServicePackageRepository servicePackageRepository,
                             TransactionHistoryRepository transactionHistoryRepository,
                             TransactionHistoryService transactionHistoryService
    ) {
        this.controlDataSource = controlDataSource;
        this.schemaInitializer = schemaInitializer;
        this.dataSourceProvider = dataSourceProvider;
        this.dbTaskRepository = dbTaskRepository;
        this.emailTaskRepository = emailTaskRepository;
        this.cloudflareTaskRepository = cloudflareTaskRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.transactionHistoryService = transactionHistoryService;
    }

    @Override
    @Transactional
    public Tenant createTenant(TenantRequest request) {
        // check code
        Tenant existing = getTenantByCodeActive(request.getCode());
        if (existing != null) {
            throw new AppException(ErrorCode.TENANT_CODE_EXISTED);
        }

        // check email
        if (isEmailExisted(request.getEmail())) {
            throw new AppException(ErrorCode.TENANT_EMAIL_EXISTED);
        }

        // check phone
        if (isPhoneExisted(request.getPhone())) {
            throw new AppException(ErrorCode.TENANT_PHONE_EXISTED);
        }

        ServicePackage servicePackage = servicePackageRepository
                .findByIdAndDeletedAtIsNull(request.getServicePackageId())
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_PACKAGE_NOT_FOUND));

        log.info("Selected package for tenant {}: {}", request.getCode(), servicePackage.getPackageName());

        String dbName = "hospital_" + request.getCode();
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .code(request.getCode())
                .dbHost(host)
                .dbPort(port)
                .dbName(dbName)
                .dbUsername(rootUsername)
                .dbPassword(rootPassword)
                .status(servicePackage.getPrice().compareTo((double) 0) > 0
                        ? Status.INACTIVE.name()    // paid → inactive
                        : Status.PENDING.name())    // free → pending until DB is ready
                .email(request.getEmail())
                .phone(request.getPhone())
                .servicePackageId(servicePackage.getId())
                .build();

        processPackagePurchase(tenant, servicePackage);
        insertTenantToControlDb(tenant);
        queueCloudflareSubdomain(tenant);

        DbTask task = DbTask.builder()
                .tenantCode(tenant.getCode())
                .action(Action.CREATE)
                .status(Status.PENDING)
                .build();
        dbTaskRepository.save(task);

        return tenant;
    }

    private boolean isEmailExisted(String email) {
        String sql = "SELECT COUNT(*) FROM tenants WHERE email = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email existence", e);
        }
        return false;
    }

    private boolean isPhoneExisted(String phone) {
        String sql = "SELECT COUNT(*) FROM tenants WHERE phone = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking phone existence", e);
        }
        return false;
    }

    @Override
    public void deleteTenant(String code) {
        Tenant tenant = getTenantByCodeActive(code);
        if (tenant == null) {
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);
        }

        String updateSql = "UPDATE tenants SET status = 'INACTIVE' WHERE code = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete tenant", e);
        }

    }

    public void insertTenantToControlDb(Tenant tenant) {
        String insertSql = "INSERT INTO tenants (id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone, service_package_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, tenant.getId());
            stmt.setString(2, tenant.getName());
            stmt.setString(3, tenant.getCode());
            stmt.setString(4, tenant.getDbHost());
            stmt.setString(5, tenant.getDbPort());
            stmt.setString(6, tenant.getDbName());
            stmt.setString(7, tenant.getDbUsername());
            stmt.setString(8, tenant.getDbPassword());
            stmt.setString(9, tenant.getStatus());
            stmt.setString(10, tenant.getEmail());
            stmt.setString(11, tenant.getPhone());
            stmt.setString(12, tenant.getServicePackageId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert tenant", e);
        }
    }

    private void queueEmail(Tenant tenant, String subject) {
        String url = "https://" + tenant.getCode() + "." + domain + "/";
        String content;

        try {
            ClassPathResource resource = new ClassPathResource("templates/welcome-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template.replace("{{name}}", tenant.getName()).replace("{{url}}", url);
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

    public void queueCloudflareSubdomain(Tenant tenant) {
        String subdomain = tenant.getCode() + "." + domain;

        cloudflareTaskRepository.save(CloudflareTask.builder()
                .id(UUID.randomUUID().toString())
                .subdomain(subdomain)
                .status(Status.PENDING)
                .retryCount(0)
                .build());

        log.info("Queued Cloudflare subdomain task for {}", subdomain);
    }

    private Tenant mapResultSetToTenant(ResultSet rs) throws SQLException {
        return Tenant.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .code(rs.getString("code"))
                .dbHost(rs.getString("db_host"))
                .dbPort(rs.getString("db_port"))
                .dbName(rs.getString("db_name"))
                .dbUsername(rs.getString("db_username"))
                .dbPassword(rs.getString("db_password"))
                .status(rs.getString("status"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .servicePackageId(rs.getString("service_package_id"))
                .build();
    }

    @Override
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM tenants";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tenants.add(mapResultSetToTenant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading tenants", e);
        }
        return tenants;
    }

    @Override
    public Tenant getTenantByCode(String code) {
        String sql = "SELECT * FROM tenants WHERE code = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTenant(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading tenant", e);
        }
    }

    @Override
//    @Cacheable(value = "activeTenants")
    public List<Tenant> getAllTenantsActive() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM tenants WHERE status = 'ACTIVE'";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tenants.add(mapResultSetToTenant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading active tenants", e);
        }
        return tenants;
    }

    @Override
    public void updateSchemaForTenants(List<String> tenantCodes) {
        for (String code : tenantCodes) {
            Tenant tenant = getTenantByCodeActive(code);
            if (tenant == null || !Status.ACTIVE.name().equalsIgnoreCase(tenant.getStatus())) continue;

            try {
                schemaInitializer.initializeSchema(tenant);
                dataSourceProvider.getDataSource(code);
            } catch (Exception e) {
                log.error("Failed to update schema for tenant {}: {}", code, e.getMessage());
            }
        }
    }

    @Override
    public void activateTenant(String code) {
        String sql = "UPDATE tenants SET status = 'ACTIVE' WHERE code = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.executeUpdate();
            log.info("Tenant {} has been activated.", code);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to activate tenant", e);
        }
    }

    @Override
    public Tenant getTenantByCodeActive(String code) {
        String sql = "SELECT * FROM tenants WHERE code = ? AND status = 'ACTIVE'";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTenant(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading tenant", e);
        }
    }

    @Override
    public Tenant purchasePackage(PurchasePackageRequest request) {
        Tenant tenant = getTenantByCodeActive(request.getTenantCode());
        if (tenant == null) {
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);
        }

        ServicePackage servicePackage = servicePackageRepository
                .findByIdAndDeletedAtIsNull(request.getPackageId())
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_PACKAGE_NOT_FOUND));

        processPackagePurchase(tenant, servicePackage);

        return tenant;
    }

    @Override
    public void updateTenantServicePackage(String tenantId, String servicePackageId) {
        String updateSql = "UPDATE tenants SET service_package_id = ? WHERE id = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, servicePackageId);
            stmt.setString(2, tenantId);
            stmt.executeUpdate();
            log.info("Updated service package for tenant {} to package {}", tenantId, servicePackageId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tenant's package", e);
        }
    }

    public PagedResponse<TenantResponse> getAllTenantsResponse(String keyword, int page, int size) {
        List<TenantResponse> responses = new ArrayList<>();
        String baseSql = "FROM tenants WHERE 1=1 ";
        String condition = "";

        if (keyword != null && !keyword.isBlank()) {
            condition = "AND (LOWER(name) LIKE ? OR LOWER(code) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ";
        }

        String sql = "SELECT * " + baseSql + condition + "LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) " + baseSql + condition;

        long totalElements = 0;
        try (Connection conn = controlDataSource.getConnection()) {
            // Count
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                int idx = 1;
                if (!condition.isEmpty()) {
                    String likeVal = "%" + keyword.toLowerCase() + "%";
                    countStmt.setString(idx++, likeVal); // name
                    countStmt.setString(idx++, likeVal); // code
                    countStmt.setString(idx++, likeVal); // email
                    countStmt.setString(idx++, likeVal); // phone
                }
                ResultSet rs = countStmt.executeQuery();
                if (rs.next()) totalElements = rs.getLong(1);
            }

            // Data
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                if (!condition.isEmpty()) {
                    String likeVal = "%" + keyword.toLowerCase() + "%";
                    stmt.setString(idx++, likeVal); // name
                    stmt.setString(idx++, likeVal); // code
                    stmt.setString(idx++, likeVal); // email
                    stmt.setString(idx++, likeVal); // phone
                }
                stmt.setInt(idx++, size);
                stmt.setInt(idx, page * size);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Tenant tenant = mapResultSetToTenant(rs);

                        Optional<ServicePackage> servicePackageOpt =
                                servicePackageRepository.findByIdAndDeletedAtIsNull(tenant.getServicePackageId());

                        TenantResponse response = TenantResponse.builder()
                                .id(tenant.getId())
                                .name(tenant.getName())
                                .code(tenant.getCode())
                                .status(tenant.getStatus())
                                .email(tenant.getEmail())
                                .phone(tenant.getPhone())
                                .servicePackageName(servicePackageOpt.map(ServicePackage::getPackageName).orElse(null))
                                .build();

                        responses.add(response);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading active tenants response", e);
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = page + 1 >= totalPages;

        return new PagedResponse<>(
                responses, page, size, totalElements, totalPages, last
        );
    }

    /**
     * Tính toán ngày kết thúc của gói dịch vụ dựa vào billingType và số lượng kỳ hạn.
     *
     * @param billingType Loại thanh toán (WEEKLY, MONTHLY, YEARLY)
     * @param quantity    Số lượng kỳ hạn (tuần/tháng/năm)
     * @param startDate   Ngày bắt đầu
     * @return            Ngày kết thúc
     */
    private LocalDateTime calculateEndDate(String billingType, Integer quantity, LocalDateTime startDate) {
        int unit = (quantity != null && quantity > 0) ? quantity : 1;

        return switch (billingType.toUpperCase()) {
            case "WEEKLY" -> startDate.plusWeeks(unit);
            case "MONTHLY" -> startDate.plusMonths(unit);
            case "YEARLY" -> startDate.plusYears(unit);
            default -> throw new AppException(ErrorCode.TRANSACTION_INVALID_BILLING_TYPE);
        };
    }

    /**
     * Xử lý đăng ký gói dịch vụ mới cho tenant.
     * Nếu tenant đã có gói hiện tại còn hiệu lực, gói mới sẽ bắt đầu sau khi gói cũ kết thúc.
     * Nếu không có gói nào hoặc gói cũ đã hết hạn, gói mới bắt đầu từ thời điểm hiện tại.
     *
     * @param tenant          Tenant mua gói
     * @param servicePackage  Gói dịch vụ cần mua
     */
    public void processPackagePurchase(Tenant tenant, ServicePackage servicePackage) {
        LocalDateTime now = LocalDateTime.now();

        // Lấy giao dịch mới nhất
        Optional<TransactionHistory> latestTransactionOpt = transactionHistoryRepository.findLatestActivePackage(tenant.getId());

        // Nếu có gói cũ → bắt đầu gói mới sau khi gói cũ kết thúc
        LocalDateTime startDate = latestTransactionOpt
                .map(TransactionHistory::getEndDate)
                .orElse(now);

        // Tính endDate của gói mới
        LocalDateTime endDate = calculateEndDate(servicePackage.getBillingType(), servicePackage.getQuantity(), startDate);

        // Tạo bản ghi lịch sử giao dịch mới
        TransactionHistoryRequest transactionHistoryRequest = TransactionHistoryRequest.builder()
                .tenantId(tenant.getId())
                .servicePackageId(servicePackage.getId())
                .price(servicePackage.getPrice())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        transactionHistoryService.create(transactionHistoryRequest);

        // Nếu chưa từng có gói, hoặc gói cũ đã hết hạn → kích hoạt ngay
        boolean shouldActivateNow = latestTransactionOpt
                .map(transactionHistory -> transactionHistory.getEndDate() == null
                        || !transactionHistory.getEndDate().isAfter(now))
                .orElse(true);

        if (shouldActivateNow) {
            updateTenantServicePackage(tenant.getId(), servicePackage.getId());
        }
    }

    @Override
    @Transactional
    public void updateTenantStatus(String code, String newStatus) {
        Tenant tenant = getTenantByCode(code);
        if (tenant == null) {
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);
        }

        String updateSql = "UPDATE tenants SET status = ? WHERE code = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, code);
            stmt.executeUpdate();
            log.info("Tenant {} status updated to {}", code, newStatus);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tenant status", e);
        }

        // Thêm bản ghi vào db_task để job xử lý
        DbTask task = DbTask.builder()
                .tenantCode(code)
                .action(Action.CREATE)   // action UPDATE
                .status(Status.PENDING)  // chờ job xử lý
                .build();
        dbTaskRepository.save(task);
    }

}
