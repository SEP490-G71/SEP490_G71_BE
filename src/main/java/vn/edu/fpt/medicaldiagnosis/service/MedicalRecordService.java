package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;

public interface MedicalRecordService {
    MedicalRecordResponse createMedicalRecord(MedicalRecordRequest requestDTO);
}
