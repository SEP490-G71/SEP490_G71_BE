package com.devintel.identityservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devintel.identityservice.dto.request.UserCreationRequest;
import com.devintel.identityservice.dto.request.UserUpdateRequest;
import com.devintel.identityservice.dto.response.UserResponse;
import com.devintel.identityservice.entity.Role;
import com.devintel.identityservice.entity.User;
import com.devintel.identityservice.exception.AppException;
import com.devintel.identityservice.exception.ErrorCode;
import com.devintel.identityservice.mapper.UserMapper;
import com.devintel.identityservice.repository.RoleRepository;
import com.devintel.identityservice.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<Role> roles = new HashSet<>();
        Role roleUser = roleRepository.findById("USER").orElseThrow(() -> new RuntimeException("Role not found"));
        roles.add(roleUser);
        user.setRoles(roles);

        user = userRepository.save(user);
        log.info("User created: {}", user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<Role> roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User updatedUser = userMapper.updateUser(user, request);

        return userMapper.toUserResponse(userRepository.save(updatedUser));
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public List<UserResponse> getUsers() {
        List<User> users = userRepository.findAll();

        return users.stream().map(userMapper::toUserResponse).collect(Collectors.toList());
    }

    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    public UserResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }
}
