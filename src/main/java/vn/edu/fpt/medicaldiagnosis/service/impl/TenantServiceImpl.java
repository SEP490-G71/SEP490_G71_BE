package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.CloudflareTask;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Action;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.CloudflareTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.DbTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
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

    @Value("${cloudflare.domain}")
    private String domain;

    @Value("${cloudflare.zone-id}")
    private String zoneId;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    @Value("${cloudflare.ip-address}")
    private String ipAddress;

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
                             CloudflareTaskRepository cloudflareTaskRepository) {
        this.controlDataSource = controlDataSource;
        this.schemaInitializer = schemaInitializer;
        this.dataSourceProvider = dataSourceProvider;
        this.dbTaskRepository = dbTaskRepository;
        this.emailTaskRepository = emailTaskRepository;
        this.cloudflareTaskRepository = cloudflareTaskRepository;
    }

    @Override
    public Tenant createTenant(TenantRequest request) {
        Tenant existing = getTenantByCode(request.getCode());
        if (existing != null) {
            if (Status.ACTIVE.name().equalsIgnoreCase(existing.getStatus())) {
                throw new AppException(ErrorCode.TENANT_CODE_EXISTED);
            }
            return reactivateTenant(existing, request);
        }

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
                .status(Status.PENDING.name())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        insertTenantToControlDb(tenant);
        queueCloudflareSubdomain(tenant);
        queueEmail(tenant, "Thông tin tài khoản");

        DbTask task = DbTask.builder()
                .tenantCode(tenant.getCode())
                .action(Action.CREATE)
                .status(Status.PENDING)
                .build();
        dbTaskRepository.save(task);

        return tenant;
    }

    @Override
    public void deleteTenant(String code) {
        Tenant tenant = getTenantByCode(code);
        if (tenant == null) {
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);
        }

        String updateSql = "UPDATE tenants SET status = 'INACTIVE' WHERE code = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to soft delete tenant", e);
        }

        DbTask task = DbTask.builder()
                .id(UUID.randomUUID().toString())
                .tenantCode(code)
                .action(Action.DROP)
                .status(Status.PENDING)
                .build();
        dbTaskRepository.save(task);
    }

    private Tenant reactivateTenant(Tenant existing, TenantRequest request) {
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setStatus(Status.ACTIVE.name());

        String updateSql = "UPDATE tenants SET name=?, status=?, email=?, phone=? WHERE code=?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, existing.getName());
            stmt.setString(2, existing.getStatus());
            stmt.setString(3, existing.getEmail());
            stmt.setString(4, existing.getPhone());
            stmt.setString(5, existing.getCode());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reactivate tenant", e);
        }

        queueEmail(existing, "Tài khoản đã khôi phục");

        DbTask task = DbTask.builder()
                .tenantCode(existing.getCode())
                .action(Action.CREATE)
                .status(Status.PENDING)
                .build();
        dbTaskRepository.save(task);

        return existing;
    }

    private void insertTenantToControlDb(Tenant tenant) {
        String insertSql = "INSERT INTO tenants (id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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

    private void queueCloudflareSubdomain(Tenant tenant) {
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
    @Cacheable(value = "activeTenants")
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
            Tenant tenant = getTenantByCode(code);
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
}
