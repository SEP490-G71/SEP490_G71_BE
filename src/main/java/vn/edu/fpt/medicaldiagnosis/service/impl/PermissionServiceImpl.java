package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.GroupedPermissionResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.PermissionMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PermissionRepository;
import vn.edu.fpt.medicaldiagnosis.service.PermissionService;
import vn.edu.fpt.medicaldiagnosis.specification.PermissionSpecification;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionMapper permissionMapper;

    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.countIncludingDeleted(request.getName()) > 0) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).collect(Collectors.toList());
    }

    @Override
    public PermissionResponse updatePermission(String id, PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));

        permission.setDescription(request.getDescription());
        permission.setGroupName(request.getGroupName());

        return permissionMapper.toPermissionResponse(permissionRepository.save(permission));
    }


    @Override
    public void deletePermission(String id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        permission.setDeletedAt(LocalDateTime.now());
        permissionRepository.save(permission);
    }


    @Override
    public List<GroupedPermissionResponse> getGroupedPermissions() {
        List<Permission> allPermissions = permissionRepository.findAll();

        return allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getGroupName))
                .entrySet().stream()
                .map(entry -> GroupedPermissionResponse.builder()
                        .groupName(entry.getKey())
                        .permissions(entry.getValue().stream()
                                .map(permissionMapper::toPermissionResponse)
                                .toList())
                        .build())
                .toList();
    }

    @Override
    public PermissionResponse getPermissionById(String id) {
        Permission permission = permissionRepository.findByNameAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toPermissionResponse(permission);
    }

    @Override
    public Page<PermissionResponse> getPermissionsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get paginated permissions");

        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortColumn).ascending() :
                Sort.by(sortColumn).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Permission> spec = PermissionSpecification.buildSpecification(filters);

        Page<Permission> permissions = permissionRepository.findAll(spec, pageable);
        return permissions.map(permissionMapper::toPermissionResponse);
    }
}
