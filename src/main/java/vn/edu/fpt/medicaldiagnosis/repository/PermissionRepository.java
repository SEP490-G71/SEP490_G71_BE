package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.fpt.medicaldiagnosis.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {}
