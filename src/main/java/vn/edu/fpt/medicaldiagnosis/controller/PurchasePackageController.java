package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PurchasePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StatisticResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.service.TransactionHistoryService;

@RestController
@RequestMapping("/purchase-packages")
@Slf4j
public class PurchasePackageController {
    @Autowired
    private TenantService tenantService;

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @PostMapping
    public ApiResponse<Tenant> purchasePackage(@RequestBody @Valid PurchasePackageRequest request) {
        return ApiResponse.<Tenant>builder()
                .message("Mua gói dịch vụ thành công")
                .result(tenantService.purchasePackage(request))
                .build();
    }

    @GetMapping("/statistics")
    public ApiResponse<StatisticResponse> getStatistics() {
        return ApiResponse.<StatisticResponse>builder()
                .result(transactionHistoryService.getBusinessStatistics())
                .build();
    }

}
