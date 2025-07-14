package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    @Query(value = "SELECT COUNT(*) FROM roles WHERE name = :name", nativeQuery = true)
    Long countIncludingDeleted(@Param("name") String name);

    Page<Role> findAll(Specification<Role> spec, Pageable pageable);

    Optional<Role> findByName(String roleName);
}
