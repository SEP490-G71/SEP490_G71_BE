package com.devintel.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devintel.identityservice.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {}
