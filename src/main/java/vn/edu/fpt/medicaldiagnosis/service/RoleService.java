package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.RolePermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;

import java.util.List;
import java.util.Map;

public interface RoleService {

    RoleResponse createRole(RoleRequest request);

    List<RoleResponse> getAllRoles();

    void deleteRole(String id);

    RoleResponse updateRole(String roleName, RoleRequest request);

    RoleResponse getById(String id);

    Page<RoleResponse> getRolesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
