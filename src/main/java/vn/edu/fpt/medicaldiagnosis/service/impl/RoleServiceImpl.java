package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.fpt.medicaldiagnosis.dto.request.RolePermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;
import vn.edu.fpt.medicaldiagnosis.entity.Role;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.PermissionMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.RoleMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PermissionRepository;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.service.RoleService;
import vn.edu.fpt.medicaldiagnosis.specification.RoleSpecification;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.countIncludingDeleted(request.getName()) > 0) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        Role role = roleMapper.toRole(request);
        if (request.getPermissions() != null) {
           List<Permission> permissions = permissionRepository.findAllById(request.getPermissions());

           if(permissions.isEmpty()) {
               throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
           }
           role.setPermissions(new HashSet<>(permissions));
        }
        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toRoleResponse).toList();
    }

    @Override
    public void deleteRole(String roleName) {
        Role role = roleRepository.findById(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }


    @Override
    public RoleResponse updateRole(String roleName, RoleRequest request) {
        Role role = roleRepository.findById(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Cập nhật mô tả (nếu có)
        role.setDescription(request.getDescription());

        // Cập nhật lại danh sách quyền nếu được gửi từ FE
        if (request.getPermissions() != null) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissions());

            if(permissions.isEmpty()) {
                throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
            }

            role.setPermissions(new HashSet<>(permissions));
        }

        Role updated = roleRepository.save(role);

        return roleMapper.toRoleResponse(updated);
    }

    @Override
    public RoleResponse getById(String roleName) {
        Role role = roleRepository.findById(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toRoleResponse(role);
    }

    @Override
    public Page<RoleResponse> getRolesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get paged roles");
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Role> spec = RoleSpecification.buildSpecification(filters);
        Page<Role> rolePage = roleRepository.findAll(spec, pageable);

        return rolePage.map(roleMapper::toRoleResponse);
    }
}
