package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentStaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentStaffResponse;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentStaffService;

import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/department-staffs")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DepartmentStaffController {

    DepartmentStaffService departmentStaffService;
    @PostMapping
    ApiResponse<List<DepartmentStaffResponse>> createDepartmentStaffs(@RequestBody @Valid DepartmentStaffCreateRequest request) {
        log.info("Controller: {}", request);
        ApiResponse<List<DepartmentStaffResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(departmentStaffService.assignStaffsToDepartment(request));
        return apiResponse;
    }

    @GetMapping("/department/{departmentId}/staffs")
    public ApiResponse<List<DepartmentStaffResponse>> getStaffsByDepartment(@PathVariable UUID departmentId) {
        List<DepartmentStaffResponse> staffs = departmentStaffService.getStaffsByDepartmentId(departmentId);
        return ApiResponse.<List<DepartmentStaffResponse>>builder()
                .result(staffs)
                .build();
    }

}
