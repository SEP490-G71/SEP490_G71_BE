package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
}
