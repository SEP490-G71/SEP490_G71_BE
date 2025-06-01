package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
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
            throw new RuntimeException("Tenant code already exists: " + request.getCode());
        }

        // Build tenant
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .code(request.getCode())
                .dbHost(request.getDbHost())
                .dbPort(request.getDbPort())
                .dbName(request.getDbName())
                .dbUsername(request.getDbUsername())
                .dbPassword(request.getDbPassword())
                .status(request.getStatus())
                .build();

        // JDBC URL không có db name để tạo db
        String adminJdbcUrl = "jdbc:mysql://" + tenant.getDbHost() + ":" + tenant.getDbPort();

        // Step 1: Tạo DB và User
        try (Connection conn = DriverManager.getConnection(adminJdbcUrl, "root", "root");
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + tenant.getDbName() + "`");
            stmt.executeUpdate("CREATE USER IF NOT EXISTS '" + tenant.getDbUsername() + "'@'%' IDENTIFIED BY '" + tenant.getDbPassword() + "'");
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON `" + tenant.getDbName() + "`.* TO '" + tenant.getDbUsername() + "'@'%'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
            log.info(" Database and user created for tenant: {}", tenant.getCode());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create DB/user: " + e.getMessage(), e);
        }

        // Step 2: Ghi vào control DB
        String insertSql = "INSERT INTO tenants (id, name, code, db_host, db_port, db_name, db_username, db_password, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            stmt.executeUpdate();
            log.info("Inserted tenant config into control DB: {}", tenant.getCode());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert tenant into control DB: " + e.getMessage(), e);
        }

        // Step 3: Sync schema + cache datasource
        try {
            schemaInitializer.initializeSchema(tenant);
            dataSourceProvider.getDataSource(tenant.getCode());
            log.info("Finished setting up tenant: {}", tenant.getCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize schema or datasource: " + e.getMessage(), e);
        }

        return tenant;
    }

    @Override
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status FROM tenants";

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
        String sql = "SELECT id, name, code, db_host, db_port, db_name, db_username, db_password, status FROM tenants WHERE code = ?";

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
