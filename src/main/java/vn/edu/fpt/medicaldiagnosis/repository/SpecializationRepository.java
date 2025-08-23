package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Specialization;

import java.util.List;
import java.util.Optional;

public interface SpecializationRepository extends JpaRepository<Specialization, String> {

    Page<Specialization> findAll(Specification<Specialization> spec, Pageable pageable);

    List<Specialization> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String trim);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    Optional<Specialization> findByIdAndDeletedAtIsNull(String id);

    Optional<Specialization> findByName(String specialty);
}
