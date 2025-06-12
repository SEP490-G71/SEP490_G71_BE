package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.RolePermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;
import vn.edu.fpt.medicaldiagnosis.service.RoleService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/roles")
@Slf4j
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping
    public ApiResponse<RoleResponse> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.createRole(request))
                .message("Role created successfully")
                .build();
    }

    @PutMapping("/{roleName}")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable String roleName,
            @RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.updateRole(roleName, request))
                .message("Role updated successfully")
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRoles())
                .build();
    }

    @DeleteMapping("/{role}")
    public ApiResponse<Void> deleteRole(@PathVariable String role) {
        roleService.deleteRole(role);
        return ApiResponse.<Void>builder()
                .message("Role deleted successfully")
                .build();
    }

    @GetMapping("/{roleName}")
    public ApiResponse<RoleResponse> getRoleById(@PathVariable String roleName) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getById(roleName))
                .build();
    }
}
