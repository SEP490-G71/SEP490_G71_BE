package vn.edu.fpt.medicaldiagnosis.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.impl.TenantServiceImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TenantSchemaInitializer {

    private final TenantServiceImpl tenantService;
    private final String sqlContent;

    public TenantSchemaInitializer(TenantServiceImpl tenantService) {
        this.tenantService = tenantService;
        this.sqlContent = loadSqlFile("/sql/tenant_schema.sql");
    }

    // Khởi tạo schema cho tất cả tenant khi app start
    @PostConstruct
    public void initAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        for (Tenant tenant : tenants) {
            try {
                initializeSchema(tenant);
            } catch (Exception e) {
                System.out.println("Failed to init schema for tenant " + tenant.getId() + ": " + e.getMessage());
            }
        }
    }

    // Cho phép gọi lại khi tenant reconnect
    public void initializeSchema(Tenant tenant) {
        try (Connection conn = DriverManager.getConnection(
                tenant.getDbUrl(), tenant.getDbUsername(), tenant.getDbPassword());
             Statement stmt = conn.createStatement()) {

            for (String query : sqlContent.split(";")) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }

            System.out.println("Schema synced for tenant: " + tenant.getId());

        } catch (Exception e) {
            System.out.println("Schema sync failed for tenant " + tenant.getId() + ": " + e.getMessage());
        }
    }

    private String loadSqlFile(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(path)), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Cannot read SQL file: " + path, e);
        }
    }
}
