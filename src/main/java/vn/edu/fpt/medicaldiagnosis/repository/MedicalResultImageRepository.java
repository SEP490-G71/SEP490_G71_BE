package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalResultImage;

import java.util.List;

public interface MedicalResultImageRepository extends JpaRepository<MedicalResultImage, String> {
    List<MedicalResultImage> findAllByMedicalResultId(String medicalResultId);

    List<MedicalResultImage> findAllByMedicalResultIdAndDeletedAtIsNull(String resultId);

}
