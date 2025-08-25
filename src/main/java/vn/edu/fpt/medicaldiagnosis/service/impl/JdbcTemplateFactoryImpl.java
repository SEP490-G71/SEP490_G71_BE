package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.JdbcTemplateFactory;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class JdbcTemplateFactoryImpl implements JdbcTemplateFactory {

    private final TenantService tenantService;

    @Override
    public JdbcTemplate create(String tenantCode) {
        Tenant tenant = tenantService.getTenantByCodeActive(tenantCode);
        if (tenant == null) {
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);
        }

        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:mysql://" + tenant.getDbHost() + ":" + tenant.getDbPort() + "/" + tenant.getDbName() + "?useSSL=false&serverTimezone=UTC")
                .username(tenant.getDbUsername())
                .password(tenant.getDbPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();

        return new JdbcTemplate(dataSource);
    }
}
