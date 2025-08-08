package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.StaffFeedback;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StaffFeedbackRepository extends JpaRepository<StaffFeedback, String> {
    Optional<StaffFeedback> findByIdAndDeletedAtIsNull(String id);

    List<StaffFeedback> findAllByDeletedAtIsNull();

    List<StaffFeedback> findAllByMedicalRecordIdAndDeletedAtIsNull(String medicalRecordId);

    List<StaffFeedback> findByDoctor_IdAndDeletedAtIsNull(String staffId);
}
