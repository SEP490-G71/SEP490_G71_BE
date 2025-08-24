package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PurchasePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateTenantStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.TenantResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ApiResponse<Tenant> createTenant(@RequestBody @Valid TenantRequest request) {
        return ApiResponse.<Tenant>builder()
                .message("Tạo tenant thành công")
                .result(tenantService.createTenant(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<TenantResponse>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<PagedResponse<TenantResponse>>builder()
                .message("Lấy danh sách tất cả tenant")
                .result(tenantService.getAllTenantsResponse(keyword, page, size))
                .build();
    }

    @GetMapping("/active")
    public ApiResponse<List<Tenant>> getAllActiveTenants() {
        return ApiResponse.<List<Tenant>>builder()
                .message("Lấy danh sách tenant đang hoạt động")
                .result(tenantService.getAllTenantsActive())
                .build();
    }

    @GetMapping("/{code}")
    public ApiResponse<Tenant> getTenantByCode(@PathVariable String code) {
        Tenant tenant = tenantService.getTenantByCode(code);
        return ApiResponse.<Tenant>builder()
                .message("Lấy thông tin tenant theo mã code")
                .result(tenant)
                .build();
    }

    @DeleteMapping("/{code}")
    public ApiResponse<Void> deleteTenant(@PathVariable String code) {
        tenantService.deleteTenant(code);
        return ApiResponse.<Void>builder()
                .message("Xoá tenant thành công")
                .build();
    }

    @PostMapping("/update-schema")
    public ApiResponse<Void> updateSchema(@RequestBody List<String> tenantCodes) {
        tenantService.updateSchemaForTenants(tenantCodes);
        return ApiResponse.<Void>builder()
                .message("Cập nhật schema cho các tenant thành công")
                .build();
    }

    @PostMapping("/purchase")
    public ApiResponse<Tenant> purchasePackage(@RequestBody @Valid PurchasePackageRequest request) {
        return ApiResponse.<Tenant>builder()
                .message("Mua gói dịch vụ thành công")
                .result(tenantService.purchasePackage(request))
                .build();
    }

    @PutMapping("/{code}")
    public ApiResponse<Void> updateTenantStatus(
            @PathVariable String code,
            @RequestBody @Valid UpdateTenantStatusRequest request
    ) {
        tenantService.updateTenantStatus(code, request.getStatus());
        return ApiResponse.<Void>builder()
                .message(String.format("Cập nhật trạng thái tenant %s thành %s thành công", code, request.getStatus()))
                .build();
    }

}
