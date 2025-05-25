package com.devintel.identityservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.devintel.identityservice.dto.request.RoleRequest;
import com.devintel.identityservice.dto.response.ApiResponse;
import com.devintel.identityservice.dto.response.RoleResponse;
import com.devintel.identityservice.service.RoleService;

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
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRoles())
                .build();
    }

    @DeleteMapping("/{role}")
    public ApiResponse<Void> deletePermission(@PathVariable String role) {
        roleService.deleteRole(role);
        return ApiResponse.<Void>builder().message("Role deleted successfully").build();
    }
}
