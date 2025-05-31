package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;
import vn.edu.fpt.medicaldiagnosis.service.impl.RoleServiceImpl;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/roles")
@Slf4j
public class RoleController {
    @Autowired
    private RoleServiceImpl roleServiceImpl;

    @PostMapping
    public ApiResponse<RoleResponse> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleServiceImpl.createRole(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleServiceImpl.getAllRoles())
                .build();
    }

    @DeleteMapping("/{role}")
    public ApiResponse<Void> deletePermission(@PathVariable String role) {
        roleServiceImpl.deleteRole(role);
        return ApiResponse.<Void>builder().message("Role deleted successfully").build();
    }
}
