package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.service.impl.PermissionServiceImpl;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/permissions")
@Slf4j
public class PermissionController {
    @Autowired
    private PermissionServiceImpl permissionServiceImpl;

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionServiceImpl.createPermission(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionServiceImpl.getAllPermissions())
                .build();
    }

    @DeleteMapping("/{permission}")
    public ApiResponse<Void> deletePermission(@PathVariable String permission) {
        permissionServiceImpl.deletePermission(permission);
        return ApiResponse.<Void>builder()
                .message("Permission deleted successfully")
                .build();
    }
}
