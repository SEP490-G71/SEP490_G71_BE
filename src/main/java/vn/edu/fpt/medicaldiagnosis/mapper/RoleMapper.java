package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import vn.edu.fpt.medicaldiagnosis.dto.request.RoleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoleResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
