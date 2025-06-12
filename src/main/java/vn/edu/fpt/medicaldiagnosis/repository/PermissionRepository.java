package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.name = :name")
    Long countIncludingDeleted(@Param("name") String name);

    Optional<Permission> findByNameAndDeletedAtIsNull(String name);

}
