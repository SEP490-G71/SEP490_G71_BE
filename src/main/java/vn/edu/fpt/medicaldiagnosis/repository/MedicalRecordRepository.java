package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;

import java.util.List;
import java.util.Optional;


public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
    Optional<MedicalRecord> findByIdAndDeletedAtIsNull(String id);

    Page<MedicalRecord> findAll(Specification<MedicalRecord> spec, Pageable pageable);

    List<MedicalRecord> findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(String patientId);

    long countByDeletedAtIsNull();
}
