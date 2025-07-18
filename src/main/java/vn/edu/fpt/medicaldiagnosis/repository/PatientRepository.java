package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    List<Patient> findAllByDeletedAtIsNull();
    Optional<Patient> findByIdAndDeletedAtIsNull(String id);

    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String phone, String id);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, String id);

    Page<Patient> findAll(Specification<Patient> spec, Pageable pageable);

    Optional<Patient> findByAccountId(String accountId);

    List<Patient> findByFullNameContainingIgnoreCaseOrPatientCodeContainingIgnoreCase(String keyword, String keyword1);

    @Query(value = "SELECT * FROM patients WHERE id IN (:ids) AND deleted_at IS NULL", nativeQuery = true)
    List<Patient> findAllById(@Param("ids") List<String> ids);

}
