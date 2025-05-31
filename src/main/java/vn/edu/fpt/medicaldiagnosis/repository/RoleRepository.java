package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.fpt.medicaldiagnosis.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {}
