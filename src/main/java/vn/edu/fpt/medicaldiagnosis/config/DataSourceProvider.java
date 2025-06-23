package vn.edu.fpt.medicaldiagnosis.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSourceProvider(@Lazy TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public DataSource getDataSource(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            log.info("Tenant ID is null or blank. Cannot resolve datasource.");
            return null;
        }

        return cache.compute(tenantId, (key, existingDs) -> {
            if (existingDs != null) {
                try (Connection conn = existingDs.getConnection()) {
                    return existingDs;
                } catch (Exception e) {
                    log.info("Lost connection to tenant '{}'. Reinitializing datasource. Reason: {}", tenantId, e.getMessage());
                    closeQuietly(existingDs);
                }
            }

            try {
                log.info("Resolving datasource for tenant '{}'", tenantId);
                Tenant tenant = tenantService.getTenantByCode(tenantId);

                if (tenant == null || tenant.getDbUrl() == null) {
                    log.info("Tenant configuration not found or missing DB URL for '{}'", tenantId);
                    return null;
                }

                HikariDataSource newDs = buildDataSource(tenant);

                try (Connection conn = newDs.getConnection()) {
                    log.info("Connected successfully to tenant '{}'", tenantId);
                }

                return newDs;

            } catch (Exception e) {
                log.info("Failed to initialize datasource for tenant '{}'. Reason: {}", tenantId, e.getMessage());
                return null;
            }
        });
    }

    private HikariDataSource buildDataSource(Tenant tenant) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(tenant.getDbUrl());
        ds.setUsername(tenant.getDbUsername());
        ds.setPassword(tenant.getDbPassword());
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

        ds.setConnectionTimeout(10000);
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setIdleTimeout(10000);
        ds.setValidationTimeout(2000);
        ds.setPoolName("TenantPool-" + tenant.getCode());

        return ds;
    }

    private void closeQuietly(DataSource ds) {
        if (ds instanceof HikariDataSource hikari) {
            try {
                hikari.close();
                log.info("Closed existing datasource pool.");
            } catch (Exception e) {
                log.info("Failed to close datasource pool. Reason: {}", e.getMessage());
            }
        }
    }
}
