package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Specialization;

import java.util.List;

public interface SpecializationRepository extends JpaRepository<Specialization, String> {
    boolean existsByNameIgnoreCase(String name);

    Page<Specialization> findAll(Specification<Specialization> spec, Pageable pageable);

    List<Specialization> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String trim);
}
