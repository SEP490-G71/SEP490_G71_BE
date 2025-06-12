package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.RolePermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;
import vn.edu.fpt.medicaldiagnosis.service.RoleService;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/roles")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;

    @PostMapping
    public ApiResponse<RoleResponse> createRole(@RequestBody @Valid RoleRequest request) {
        log.info("Controller: create role with request {}", request);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.createRole(request))
                .message("Role created successfully")
                .build();
    }

    @PutMapping("/{roleName}")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable String roleName,
            @RequestBody @Valid RoleRequest request) {
        log.info("Controller: update role {} with request {}", roleName, request);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.updateRole(roleName, request))
                .message("Role updated successfully")
                .build();
    }

    @DeleteMapping("/{role}")
    public ApiResponse<String> deleteRole(@PathVariable String role) {
        log.info("Controller: delete role {}", role);
        roleService.deleteRole(role);
        return ApiResponse.<String>builder()
                .message("Role deleted successfully")
                .build();
    }

    @GetMapping("/{roleName}")
    public ApiResponse<RoleResponse> getRoleById(@PathVariable String roleName) {
        log.info("Controller: get role by name {}", roleName);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getById(roleName))
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        log.info("Controller: get all roles");
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRoles())
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<RoleResponse>> getRolesPaged(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get roles paged with filters {}", filters);

        Page<RoleResponse> result = roleService.getRolesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<RoleResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<RoleResponse>>builder()
                .result(response)
                .build();
    }

}
