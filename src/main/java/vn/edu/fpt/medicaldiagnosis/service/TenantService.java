package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;

import java.util.List;

public interface TenantService {
    List<Tenant> getAllTenants();

    Tenant getTenantByCode(String id);

    Tenant createTenant(TenantRequest tenant);
}
