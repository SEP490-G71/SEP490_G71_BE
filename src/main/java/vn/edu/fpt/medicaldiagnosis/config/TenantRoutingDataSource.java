package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;

import javax.sql.DataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final DataSourceProvider provider;
    private final DataSource dummyDataSource;

    public TenantRoutingDataSource(DataSourceProvider provider, DataSource dummyDataSource) {
        this.provider = provider;
        this.dummyDataSource = dummyDataSource;
    }
    @Override
    protected DataSource determineTargetDataSource() {
        String tenantId = TenantContext.getTenantId();

        // Tenant ID is null or blank → use control_db
        if (tenantId == null || tenantId.isBlank()) {
            return dummyDataSource;
        }

        try {
            DataSource ds = provider.getDataSource(tenantId);
            if (ds != null) {
                return ds;
            }
            System.out.println("Datasource unavailable for tenant: " + tenantId + " → using dummy datasource.");
        } catch (Exception e) {
            System.err.println("Exception getting datasource for tenant " + tenantId + ": " + e.getMessage());
        }

        return dummyDataSource;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        System.out.println("Routing for tenantId: " + tenantId);
        return tenantId;
    }
}
