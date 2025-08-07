package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalServiceFeedback;

import java.util.List;
import java.util.Optional;

public interface MedicalServiceFeedbackRepository extends JpaRepository<MedicalServiceFeedback, String> {
    Optional<MedicalServiceFeedback> findByIdAndDeletedAtIsNull(String id);
    List<MedicalServiceFeedback> findAllByDeletedAtIsNull();
}
