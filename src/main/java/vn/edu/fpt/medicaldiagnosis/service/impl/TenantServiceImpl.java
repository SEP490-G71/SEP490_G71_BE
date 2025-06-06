package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        // Check trùng code
        if (getTenantByCode(request.getCode()) != null) {
            throw new AppException(ErrorCode.TENANT_CODE_EXISTED);
        }
        String dbName = "hospital_" + request.getCode();
        // Build tenant
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .code(request.getCode())
                .dbHost(host)
                .dbPort(port)
                .dbName(dbName)
                .dbUsername(username)
                .dbPassword(password)
                .status("ACTIVE")
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        // Step 1: Tạo subdomain trên Cloudflare trước
        createSubdomainForTenant(tenant);

        // Step 2: Tạo database và user
        String adminJdbcUrl = "jdbc:mysql://" + tenant.getDbHost() + ":" + tenant.getDbPort();
        try (Connection conn = DriverManager.getConnection(adminJdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + tenant.getDbName() + "`");
            stmt.executeUpdate("CREATE USER IF NOT EXISTS '" + tenant.getDbUsername() + "'@'%' IDENTIFIED BY '" + tenant.getDbPassword() + "'");
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON `" + tenant.getDbName() + "`.* TO '" + tenant.getDbUsername() + "'@'%'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
            log.info("Database and user created for tenant: {}", tenant.getCode());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database or user: " + e.getMessage(), e);
        }

        // Step 3: Ghi vào control DB
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
            log.info("Inserted tenant config into control DB: {}", tenant.getCode());
            String url = "https://"+tenant.getCode()+".datnd.id.vn/";
            emailService.sendSimpleMail(tenant.getEmail(), "Thông tin tài khoản", tenant.getName(), url);
            log.info("Email sent to: {}", tenant.getEmail());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert tenant into control DB: " + e.getMessage(), e);
        }

        // Step 4: Sync schema + cache datasource
        try {
            schemaInitializer.initializeSchema(tenant);
            dataSourceProvider.getDataSource(tenant.getCode());
            log.info("Schema initialized and datasource cached for tenant: {}", tenant.getCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize schema or datasource: " + e.getMessage(), e);
        }

        return tenant;
    }

    private void createSubdomainForTenant(Tenant tenant) {
        try {
            String subdomain = tenant.getCode() + "." + domain;

            URL url = new URL("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + apiToken);
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String jsonBody = """
            {
                "type": "A",
                "name": "%s",
                "content": "%s",
                "ttl": 1,
                "proxied": true
            }
            """.formatted(subdomain, ipAddress);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                log.info("Subdomain created on Cloudflare: {}", subdomain);
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    log.info("Failed to create subdomain. Cloudflare response: {}", response);
                }
            }

        } catch (Exception e) {
            log.info("Error while creating subdomain on Cloudflare for tenant {}: {}", tenant.getCode(), e.getMessage());
        }
    }

    @Override
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status, email, phone FROM tenants";

        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Tenant tenant = Tenant.builder()
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
                tenants.add(tenant);
            }

            log.info("Total tenants loaded: {}", tenants.size());

        } catch (SQLException e) {
            log.info("Error loading tenants: {}", e.getMessage());
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
                    Tenant tenant = Tenant.builder()
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

                    log.info("Tenant loaded by code: {}", code);
                    return tenant;
                } else {
//                    log.info("Tenant not found with code: {}", code);
//                    throw new RuntimeException("Tenant not found: " + code);
                    return null;
                }
            }

        } catch (SQLException e) {
            log.info("Failed to load tenant with code {}: {}", code, e.getMessage());
            throw new RuntimeException("Error loading tenant", e);
        }
    }
}
