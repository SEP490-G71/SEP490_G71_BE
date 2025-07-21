package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordOrderResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public interface MedicalRecordService {
    MedicalResponse createMedicalRecord(MedicalRequest requestDTO);

    MedicalRecordDetailResponse getMedicalRecordDetail(String recordId);

    List<MedicalRecordResponse> getMedicalRecordHistory(String patientId);

    Page<MedicalRecordResponse> getMedicalRecordsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    ByteArrayInputStream generateMedicalRecordPdf(String invoiceId);

    MedicalRecordDetailResponse updateMedicalRecord(String recordId, UpdateMedicalRecordRequest request);

    List<MedicalRecordOrderResponse> getOrdersByDepartment(String departmentId);
}
