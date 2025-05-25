package com.devintel.identityservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.devintel.identityservice.dto.request.UserCreationRequest;
import com.devintel.identityservice.dto.request.UserUpdateRequest;
import com.devintel.identityservice.dto.response.UserResponse;
import com.devintel.identityservice.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    User updateUser(@MappingTarget User user, UserUpdateRequest request);
}
