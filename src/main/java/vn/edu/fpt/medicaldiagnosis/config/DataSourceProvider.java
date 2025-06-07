package vn.edu.fpt.medicaldiagnosis.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DataSourceProvider {

    private final TenantService tenantService;
    private final TenantSchemaInitializer schemaInitializer;
    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSourceProvider(TenantService tenantService, TenantSchemaInitializer schemaInitializer) {
        this.tenantService = tenantService;
        this.schemaInitializer = schemaInitializer;
    }

    public DataSource getDataSource(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            log.info("Tenant ID is null or blank — cannot resolve.");
            return null;
        }

        DataSource cached = cache.get(tenantId);
        if (cached != null) {
            try (Connection conn = cached.getConnection()) {
                return cached;
            } catch (Exception e) {
                log.info("Lost connection to tenant " + tenantId + ": " + e.getMessage());
                cache.remove(tenantId);
            }
        }

        try {
            Tenant tenant = tenantService.getTenantByCode(tenantId);
            if (tenant == null || tenant.getDbUrl() == null) {
                log.info("No config found for tenant: " + tenantId);
                return null;
            }

            log.info("Connecting to tenant DB: " + tenant.getDbName());

            HikariDataSource ds = buildDataSource(tenant);

            try (Connection conn = ds.getConnection()) {
                log.info("Connected to tenant DB: " + tenantId);
                cache.put(tenantId, ds);
                schemaInitializer.initializeSchema(tenant);
                return ds;
            }

        } catch (Exception e) {
            log.info("Failed to init datasource for tenant " + tenantId + ": " + e.getMessage());
            return null;
        }
    }

    private HikariDataSource buildDataSource(Tenant tenant) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(tenant.getDbUrl() );
        ds.setUsername(tenant.getDbUsername());
        ds.setPassword(tenant.getDbPassword());
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

        ds.setConnectionTimeout(3000);
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setIdleTimeout(10000);
        ds.setValidationTimeout(2000);

        return ds;
    }
}
