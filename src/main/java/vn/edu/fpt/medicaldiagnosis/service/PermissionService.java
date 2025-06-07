package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.GroupedPermissionResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {

    PermissionResponse createPermission(PermissionRequest request);

    List<PermissionResponse> getAllPermissions();

    PermissionResponse updatePermission(String id, PermissionRequest request);

    void deletePermission(String id);

    List<GroupedPermissionResponse> getGroupedPermissions();
}
