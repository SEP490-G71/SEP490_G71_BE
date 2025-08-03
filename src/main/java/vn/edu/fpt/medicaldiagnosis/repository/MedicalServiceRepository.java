package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, String> {
    List<MedicalService> findAllByDeletedAtIsNull();

    Optional<MedicalService> findByIdAndDeletedAtIsNull(String id);

    Page<MedicalService> findAll(Specification<MedicalService> spec, Pageable pageable);

    List<MedicalService> findByDepartmentIdAndDeletedAtIsNull(String departmentId);

    List<MedicalService> findAllByDepartment_Id(String trim);

    Optional<MedicalService> findFirstByDepartmentIdAndDefaultServiceTrueAndDeletedAtIsNull(String departmentId);
}
