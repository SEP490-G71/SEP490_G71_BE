package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PurchasePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;

import java.util.List;

public interface TenantService {
    List<Tenant> getAllTenants();

    Tenant getTenantByCode(String id);

    Tenant createTenant(TenantRequest tenant);

    void updateSchemaForTenants(List<String> tenantCodes);

    void deleteTenant(String code);

    List<Tenant> getAllTenantsActive();

    void activateTenant(String code);

    Tenant getTenantByCodeActive(String id);

    Tenant purchasePackage(PurchasePackageRequest request);
}
