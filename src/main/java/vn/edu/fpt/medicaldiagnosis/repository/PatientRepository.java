package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    List<Patient> findAllByDeletedAtIsNull();
    Optional<Patient> findByIdAndDeletedAtIsNull(String id);

    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    @Query(value = "SELECT MAX(queue_order) FROM patients WHERE queue_id = :queueId", nativeQuery = true)
    Long findMaxQueueOrderByQueueId(@Param("queueId") String queueId);
}
