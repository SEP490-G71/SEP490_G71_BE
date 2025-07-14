package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentType;

public interface DepartmentTypeRepository extends JpaRepository<DepartmentType, String> {
    Page<DepartmentType> findAll(Specification<DepartmentType> spec, Pageable pageable);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
