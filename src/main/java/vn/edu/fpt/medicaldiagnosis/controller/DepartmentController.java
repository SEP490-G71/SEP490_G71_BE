package vn.edu.fpt.medicaldiagnosis.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.AssignStaffRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/departments")
@Slf4j
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    ApiResponse<DepartmentResponse> createDepartment(@RequestBody @Valid DepartmentCreateRequest request) {
        log.info("Controller: {}", request);
        ApiResponse<DepartmentResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(departmentService.createDepartment(request));
        return apiResponse;
    }
    @GetMapping
    public ApiResponse<PagedResponse<DepartmentResponse>> getDepartments(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<DepartmentResponse> result = departmentService.getDepartmentsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<DepartmentResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<DepartmentResponse>>builder().result(response).build();
    }

    @GetMapping("/all")
    ApiResponse<List<DepartmentResponse>> getAllDepartments() {
        log.info("Controller: get all departments");
        return ApiResponse.<List<DepartmentResponse>>builder()
                .result(departmentService.getAllDepartments())
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<DepartmentDetailResponse> getDepartmentById(@PathVariable String id) {
        log.info("Controller: get department by id: {}", id);
        return ApiResponse.<DepartmentDetailResponse>builder()
                .result(departmentService.getDepartmentById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDepartment(@PathVariable String id) {
        log.info("Controller: delete department {}", id);
        departmentService.deleteDepartment(id);
        return ApiResponse.<String>builder()
                .message("Department deleted successfully.")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(@PathVariable String id, @RequestBody @Valid DepartmentUpdateRequest request) {
        log.info("Controller: update department {}", id);
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.updateDepartment(id, request))
                .build();
    }

    @PostMapping("/{departmentId}/assign-staffs")
    public ApiResponse<DepartmentDetailResponse> assignStaffsToDepartment(
            @PathVariable String departmentId,
            @RequestBody @Valid AssignStaffRequest request) {
        DepartmentDetailResponse result = departmentService.assignStaffsToDepartment(departmentId, request);
        return ApiResponse.<DepartmentDetailResponse>builder().result(result).build();
    }

    // Lấy phòng ban của nhân viên hiện tại
    // Lấy phòng ban của nhân viên hiện tại (dựa vào authentication)
    @GetMapping("/me")
    public ApiResponse<DepartmentResponse> getMyDepartment(Authentication auth) {
        String username = auth.getName();
        DepartmentResponse result = departmentService.getMyDepartment(username);
        return ApiResponse.<DepartmentResponse>builder().result(result).build();
    }

    @GetMapping("/search")
    public ApiResponse<List<DepartmentResponse>> getByTypeRoomSpecialization(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String specializationId
    ) {
        log.info("Controller: search department with type={}, room={}, specializationId={}", type, roomNumber, specializationId);
        List<DepartmentResponse> response = departmentService.getByTypeAndRoomNumberAndSpecializationId(type, roomNumber, specializationId);
        return ApiResponse.<List<DepartmentResponse>>builder().result(response).build();
    }

}
