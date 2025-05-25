package com.devintel.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devintel.identityservice.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {}
