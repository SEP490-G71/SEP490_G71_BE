package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.GroupedPermissionResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.service.PermissionService;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/permissions")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class PermissionController {

    PermissionService permissionService;

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@RequestBody @Valid PermissionRequest request) {
        log.info("Controller: create permission {}", request);
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.createPermission(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<PermissionResponse>> getPermissions(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<PermissionResponse> result = permissionService.getPermissionsPaged(filters, page, size, sortBy, sortDir);
        PagedResponse<PermissionResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<PermissionResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        log.info("Controller: get all permissions");
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAllPermissions())
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionResponse> updatePermission(
            @PathVariable String id,
            @RequestBody @Valid PermissionRequest request) {

        log.info("Controller: update permission {}", id);
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.updatePermission(id, request))
                .message("Permission updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePermission(@PathVariable String id) {
        log.info("Controller: delete permission {}", id);
        permissionService.deletePermission(id);
        return ApiResponse.<String>builder()
                .message("Permission deleted successfully")
                .build();
    }

    @GetMapping("/grouped")
    public ApiResponse<List<GroupedPermissionResponse>> getGroupedPermissions() {
        log.info("Controller: get grouped permissions");
        return ApiResponse.<List<GroupedPermissionResponse>>builder()
                .result(permissionService.getGroupedPermissions())
                .message("Grouped permissions retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionResponse> getPermissionById(@PathVariable String id) {
        log.info("Controller: get permission by id: {}", id);
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.getPermissionById(id))
                .build();
    }
}
