package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, String> {
    boolean existsByRoomNumberAndDeletedAtIsNull(String roomNumber);
    List<Department> findAllByDeletedAtIsNull();
    Optional<Department> findByIdAndDeletedAtIsNull(String id);

    Page<Department> findAll(Specification<Department> spec, Pageable pageable);

    @Query(value = "SELECT * FROM departments " +
            "WHERE type = :type " +
            "AND room_number = :roomNumber " +
            "AND deleted_at IS NULL " +
            "LIMIT 1", nativeQuery = true)
    Optional<Department> findByTypeAndRoomNumber(
            @Param("type") String type,
            @Param("roomNumber") String roomNumber
    );
}
