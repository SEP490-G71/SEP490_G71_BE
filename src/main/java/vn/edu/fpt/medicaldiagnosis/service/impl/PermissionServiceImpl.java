package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;
import vn.edu.fpt.medicaldiagnosis.mapper.PermissionMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PermissionRepository;
import vn.edu.fpt.medicaldiagnosis.service.PermissionService;

@Service
public class PermissionServiceImpl implements PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionMapper permissionMapper;

    public PermissionResponse createPermission(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).collect(Collectors.toList());
    }

    public void deletePermission(String id) {
        Permission permission =
                permissionRepository.findById(id).orElseThrow(() -> new RuntimeException("Permission not found"));
        permissionRepository.deleteById(permission.getName());
    }
}
