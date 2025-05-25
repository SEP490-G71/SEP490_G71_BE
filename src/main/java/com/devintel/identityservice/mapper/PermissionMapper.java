package com.devintel.identityservice.mapper;

import org.mapstruct.Mapper;

import com.devintel.identityservice.dto.request.PermissionRequest;
import com.devintel.identityservice.dto.response.PermissionResponse;
import com.devintel.identityservice.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
