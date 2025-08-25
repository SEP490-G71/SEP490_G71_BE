package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantDataSourceCleaner {

    private final TenantService tenantService;
    private final DataSourceProvider dataSourceProvider;

    @Scheduled(fixedDelay = 5000)
    public void cleanInactiveTenants() {
        // 1. Reset pool cho tenants INACTIVE
        List<Tenant> inactiveTenants = tenantService.getInactiveTenants();
        for (Tenant t : inactiveTenants) {
            dataSourceProvider.resetDataSource(t.getCode());
        }

        // 2. Đảm bảo tenants ACTIVE có kết nối
        List<Tenant> activeTenants = tenantService.getAllTenantsActive();
        for (Tenant t : activeTenants) {
            dataSourceProvider.ensureDataSource(t.getCode());
        }
    }
}
