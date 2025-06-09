package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {
    @Query(value = "SELECT COUNT(*) FROM roles WHERE name = :name", nativeQuery = true)
    Long countIncludingDeleted(@Param("name") String name);

}
