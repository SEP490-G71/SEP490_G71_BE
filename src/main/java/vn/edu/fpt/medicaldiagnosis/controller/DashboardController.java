package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DashboardOverviewResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceDetailResponse;
import vn.edu.fpt.medicaldiagnosis.service.DashboardService;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/dashboards")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DashboardController {
    DashboardService dashboardService;
    @GetMapping()
    public ApiResponse<DashboardOverviewResponse> getDashboard() {
        log.info("Service: get dashboard overview");
        DashboardOverviewResponse response = dashboardService.getDashboardOverview();
        return ApiResponse.<DashboardOverviewResponse>builder()
                .result(response)
                .build();
    }
}
