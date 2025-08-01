package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DefaultDataSeeder;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.DbTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.DbTaskService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbTaskServiceImpl implements DbTaskService {

    private final TenantService tenantService;
    private final DbTaskRepository dbTaskRepository;
    private final TenantSchemaInitializer tenantSchemaInitializer;
    private final DataSourceProvider dataSourceProvider;
    private final DefaultDataSeeder defaultDataSeeder;

    @Value("${database.host}")
    private String host;

    @Value("${database.port}")
    private String port;

    @Value("${spring.datasource.control.username}")
    private String rootUsername;

    @Value("${spring.datasource.control.password}")
    private String rootPassword;

    @Override
    public void createDatabase(String tenantCode) throws Exception {
        Tenant tenant = tenantService.getTenantByCode(tenantCode);
        if (tenant == null) {
            throw new Exception("Tenant not found: " + tenantCode);
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, rootUsername, rootPassword);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + tenant.getDbName() + "`");
            stmt.executeUpdate("CREATE USER IF NOT EXISTS '" + tenant.getDbUsername() + "'@'%' IDENTIFIED BY '" + tenant.getDbPassword() + "'");
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON `" + tenant.getDbName() + "`.* TO '" + tenant.getDbUsername() + "'@'%'");
            stmt.executeUpdate("FLUSH PRIVILEGES");

            log.info("Database created successfully for tenant {}", tenantCode);

            tenantSchemaInitializer.initializeSchema(tenant);
            dataSourceProvider.getDataSource(tenant.getCode());

            defaultDataSeeder.seedDefaultData(tenant);

            // Cập nhật trạng thái ACTIVE sau khi tạo DB
            tenantService.activateTenant(tenantCode);

        } catch (Exception e) {
            log.error("Error while creating database for tenant {}: {}", tenantCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void dropDatabase(String tenantCode) throws Exception {
        Tenant tenant = tenantService.getTenantByCodeActive(tenantCode);
        if (tenant == null) {
            throw new Exception("Tenant not found: " + tenantCode);
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, rootUsername, rootPassword);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DROP DATABASE IF EXISTS `" + tenant.getDbName() + "`");
            log.info("Database dropped for tenant {}", tenantCode);

        } catch (Exception e) {
            log.error("Error while dropping database for tenant {}: {}", tenantCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<DbTask> findByStatus(Status status) {
        try {
            return dbTaskRepository.findByStatus(status);
        } catch (Exception e) {
            log.error("Error while fetching tasks by status {}: {}", status, e.getMessage(), e);
            throw e;
        }
    }
}
