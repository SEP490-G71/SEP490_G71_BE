package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalResult;

import java.util.List;

public interface MedicalResultRepository extends JpaRepository<MedicalResult, String> {
    List<MedicalResult> findAllByMedicalOrderIdAndDeletedAtIsNull(String orderId);
}
