package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResponseDTO;

import java.util.Map;

public interface MedicalRecordService {
    MedicalResponseDTO createMedicalRecord(MedicalRequestDTO requestDTO);

    MedicalRecordDetailResponse getMedicalRecordDetail(String recordId);

    Page<MedicalRecordResponse> getMedicalRecordsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

}
