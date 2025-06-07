package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;

import vn.edu.fpt.medicaldiagnosis.dto.request.PermissionRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PermissionResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
