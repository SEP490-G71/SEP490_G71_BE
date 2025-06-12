package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.GroupedPermissionResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.service.PermissionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/permissions")
@Slf4j
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.createPermission(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAllPermissions())
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionResponse> updatePermission(
            @PathVariable String id,
            @RequestBody PermissionRequest request) {

        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.updatePermission(id, request))
                .message("Permission updated successfully")
                .build();
    }

    @DeleteMapping("/{permission}")
    public ApiResponse<Void> deletePermission(@PathVariable String permission) {
        permissionService.deletePermission(permission);
        return ApiResponse.<Void>builder()
                .message("Permission deleted successfully")
                .build();
    }

    @GetMapping("/grouped")
    public ApiResponse<List<GroupedPermissionResponse>> getGroupedPermissions() {
        List<GroupedPermissionResponse> grouped = permissionService.getGroupedPermissions();
        return ApiResponse.<List<GroupedPermissionResponse>>builder()
                .result(grouped)
                .message("Grouped permissions retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionResponse> getPermissionById(@PathVariable String id) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.getPermissionById(id))
                .build();
    }
}
