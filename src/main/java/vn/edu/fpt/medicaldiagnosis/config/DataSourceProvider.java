package vn.edu.fpt.medicaldiagnosis.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.impl.TenantServiceImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSourceProvider {

    private final TenantServiceImpl tenantService;

    private final TenantSchemaInitializer schemaInitializer;

    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSourceProvider(TenantServiceImpl tenantService, TenantSchemaInitializer schemaInitializer) {
        this.tenantService = tenantService;
        this.schemaInitializer = schemaInitializer;
    }

    public DataSource getDataSource(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            System.out.println("Tenant ID is null or blank — cannot resolve.");
            return null;
        }

        // Nếu đã cache → kiểm tra kết nối
        DataSource cached = cache.get(tenantId);
        if (cached != null) {
            try (Connection conn = cached.getConnection()) {
                return cached;
            } catch (Exception e) {
                System.out.println("Lost connection to tenant " + tenantId + ": " + e.getMessage());
                cache.remove(tenantId); // Xóa cache → tạo lại phía dưới
            }
        }

        // Tạo mới nếu chưa có
        try {
            Tenant tenant = tenantService.getTenantById(tenantId);
            if (tenant == null) {
                System.out.println("No config found for tenant: " + tenantId);
                return null;
            }

            // Cấu hình HikariDataSource với timeout
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(tenant.getDbUrl());
            ds.setUsername(tenant.getDbUsername());
            ds.setPassword(tenant.getDbPassword());
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // ⏱ Timeout tối đa khi kết nối (ms)
            ds.setConnectionTimeout(3000);   // 3s kết nối
            ds.setMaximumPoolSize(5);
            ds.setMinimumIdle(1);
            ds.setIdleTimeout(10000);        // 10s idle timeout
            ds.setValidationTimeout(2000);   // 2s cho validation

            try (Connection conn = ds.getConnection()) {
                System.out.println("Connected to tenant DB: " + tenantId);
                cache.put(tenantId, ds);

                // Gọi lại schema sync nếu tenant được reconnect
                schemaInitializer.initializeSchema(tenant);

                return ds;
            }

        } catch (Exception e) {
            System.out.println("Failed to init datasource for tenant " + tenantId + ": " + e.getMessage());
            return null;
        }
    }
}
