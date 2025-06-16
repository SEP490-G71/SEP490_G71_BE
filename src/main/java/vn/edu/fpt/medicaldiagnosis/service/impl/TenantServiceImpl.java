package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    private final DataSource controlDataSource;
    private final TenantSchemaInitializer schemaInitializer;
    private final DataSourceProvider dataSourceProvider;

    @Autowired
    private EmailService emailService;

    @Value("${cloudflare.zone-id}")
    private String zoneId;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    @Value("${cloudflare.ip-address}")
    private String ipAddress;

    @Value("${cloudflare.domain}")
    private String domain;

    @Value("${database.port}")
    private String port;

    @Value("${database.host}")
    private String host;

    @Value("${spring.datasource.control.username}")
    private String username;

    @Value("${spring.datasource.control.password}")
    private String password;

    @Autowired
    public TenantServiceImpl(@Qualifier("controlDataSource") DataSource controlDataSource,
                             @Lazy TenantSchemaInitializer schemaInitializer,
                             @Lazy DataSourceProvider dataSourceProvider) {
        this.controlDataSource = controlDataSource;
        this.schemaInitializer = schemaInitializer;
        this.dataSourceProvider = dataSourceProvider;
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
                .dbUsername(username)
                .dbPassword(password)
                .status(Status.ACTIVE.name())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        createSubdomainForTenant(tenant);
        recreateDatabaseAndUser(tenant);
        insertTenantToControlDb(tenant);
        schemaInitializer.initializeSchema(tenant);
        dataSourceProvider.getDataSource(tenant.getCode());
        sendTenantEmail(tenant, "Thông tin tài khoản");

        return tenant;
    }

    @Override
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone FROM tenants";

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
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone FROM tenants WHERE code = ?";

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

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS `" + tenant.getDbName() + "`");
        } catch (SQLException e) {
            log.warn("Failed to drop DB for tenant {}: {}", code, e.getMessage());
        }
    }

    @Override
    public void updateSchemaForTenants(List<String> tenantCodes) {
        for (String code : tenantCodes) {
            Tenant tenant = getTenantByCode(code);
            if (tenant == null || !Status.ACTIVE.name().equalsIgnoreCase(tenant.getStatus())) continue;

            updateSchemaAsync(tenant);
        }
    }

    private Tenant reactivateTenant(Tenant existing, TenantRequest request) {
        if (request != null) {
            existing.setName(request.getName());
            existing.setEmail(request.getEmail());
            existing.setPhone(request.getPhone());
        }
        existing.setStatus(Status.ACTIVE.name());

        recreateDatabaseAndUser(existing);

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

        createSubdomainForTenant(existing);
        schemaInitializer.initializeSchema(existing);
        dataSourceProvider.getDataSource(existing.getCode());
        sendTenantEmail(existing, "Tài khoản đã khôi phục");
        return existing;
    }

    private void recreateDatabaseAndUser(Tenant tenant) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + tenant.getDbName() + "`");
            stmt.executeUpdate("CREATE USER IF NOT EXISTS '" + tenant.getDbUsername() + "'@'%' IDENTIFIED BY '" + tenant.getDbPassword() + "'");
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON `" + tenant.getDbName() + "`.* TO '" + tenant.getDbUsername() + "'@'%'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to (re)create DB/user", e);
        }
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

    private void sendTenantEmail(Tenant tenant, String subject) {
        sendTenantEmailAsync(tenant, subject);
    }

    @Async
    public void sendTenantEmailAsync(Tenant tenant, String subject) {
        String url = "https://" + tenant.getCode() + "." + domain + "/";
        emailService.sendSimpleMail(tenant.getEmail(), subject, tenant.getName(), url);
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

    private void createSubdomainForTenant(Tenant tenant) {
        createSubdomainForTenantAsync(tenant);
    }

    @Async
    public void createSubdomainForTenantAsync(Tenant tenant) {
        try {
            String subdomain = tenant.getCode() + "." + domain;
            URL url = new URL("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + apiToken);
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String body = String.format("""
                {
                  \"type\": \"A\",
                  \"name\": \"%s\",
                  \"content\": \"%s\",
                  \"ttl\": 1,
                  \"proxied\": true
                }
                """, subdomain, ipAddress);

            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes("utf-8"));
            }

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("Cloudflare error creating subdomain {}: {}", subdomain, code);
            } else {
                log.info("Created subdomain on Cloudflare: {}", subdomain);
            }
        } catch (Exception e) {
            log.warn("Error creating subdomain: {}", e.getMessage());
        }
    }

    @Async
    public void updateSchemaAsync(Tenant tenant) {
        try {
            schemaInitializer.initializeSchema(tenant);
            dataSourceProvider.getDataSource(tenant.getCode());
        } catch (Exception e) {
            log.error("Failed to update schema for tenant {}: {}", tenant.getCode(), e.getMessage());
        }
    }

    @Override
    public List<Tenant> getAllTenantsActive() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone FROM tenants WHERE status = 'ACTIVE'";

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

}
