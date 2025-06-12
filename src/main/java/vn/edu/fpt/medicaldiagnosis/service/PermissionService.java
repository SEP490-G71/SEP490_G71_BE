package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.GroupedPermissionResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;

import java.util.List;
import java.util.Map;

public interface PermissionService {

    PermissionResponse createPermission(PermissionRequest request);

    List<PermissionResponse> getAllPermissions();

    PermissionResponse updatePermission(String id, PermissionRequest request);

    void deletePermission(String id);

    List<GroupedPermissionResponse> getGroupedPermissions();

    PermissionResponse getPermissionById(String id);

    Page<PermissionResponse> getPermissionsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
