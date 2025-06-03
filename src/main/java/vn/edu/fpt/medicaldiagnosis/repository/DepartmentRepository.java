package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    boolean existsByRoomNumberAndDeletedAtIsNull(String roomNumber);
    List<Department> findAllByDeletedAtIsNull();
    Optional<Department> findByIdAndDeletedAtIsNull(UUID id);

    Page<Department> findAll(Specification<Department> spec, Pageable pageable);
}
