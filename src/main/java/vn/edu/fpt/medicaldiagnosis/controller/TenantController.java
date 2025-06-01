package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantRequest request) {
        Tenant created = tenantService.createTenant(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantByCode(@PathVariable String code) {
        Tenant tenant = tenantService.getTenantByCode(code);
        return ResponseEntity.ok(tenant);
    }
}
