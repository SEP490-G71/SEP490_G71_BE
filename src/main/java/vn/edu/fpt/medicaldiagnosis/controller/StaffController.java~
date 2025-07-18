package vn.edu.fpt.medicaldiagnosis.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.service.StaffService;

import java.util.List;
import java.util.Map;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/staffs")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class StaffController {
    StaffService staffService;

    @PostMapping
    ApiResponse<StaffResponse> createDepartment(@RequestBody @Valid StaffCreateRequest request) {
        log.info("Controller: {}", request);
        ApiResponse<StaffResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(staffService.createStaff(request));
        return apiResponse;
    }

    @GetMapping
    public ApiResponse<PagedResponse<StaffResponse>> getStaffs(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Controller: get staffs with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);
        Page<StaffResponse> result = staffService.getStaffsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<StaffResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
        return ApiResponse.<PagedResponse<StaffResponse>>builder().result(response).build();
    }

    @GetMapping("/all")
    ApiResponse<List<StaffResponse>> getAllStaffs() {
        log.info("Controller: get all departments");
        return ApiResponse.<List<StaffResponse>>builder()
                .result(staffService.getAllStaffs())
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<StaffResponse> getStaffById(@PathVariable String id) {
        log.info("Controller: get department by id: {}", id);
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.getStaffById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteStaff(@PathVariable String id) {
        log.info("Controller: delete department {}", id);
        staffService.deleteStaff(id);
        return ApiResponse.<String>builder()
                .message("Staff deleted successfully.")
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<StaffResponse> updateStaff(@PathVariable String id, @RequestBody @Valid StaffUpdateRequest request) {
        log.info("Controller: update department {}", id);
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.updateStaff(id, request))
                .build();
    }

    @GetMapping("/unassigned")
    ApiResponse<List<StaffResponse>> getUnassignedStaffs() {
        log.info("Controller: get unassigned staffs");
        return ApiResponse.<List<StaffResponse>>builder()
                .result(staffService.getStaffNotAssignedToAnyDepartment())
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<List<StaffResponse>> searchStaffs(
            @RequestParam(value = "search", required = false) String keyword) {

        List<StaffResponse> results;

        if (keyword == null || keyword.isBlank()) {
            results = staffService.getAllStaffs();
        } else {
            results = staffService.searchByNameOrCode(keyword);
        }

        return ApiResponse.<List<StaffResponse>>builder()
                .result(results)
                .build();
    }
}
