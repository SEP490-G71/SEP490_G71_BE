package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.*;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;

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

    Page<MedicalRecordResponse> getMedicalRecordsByRoomNumber(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    MedicalRecordDetailResponse completeMedicalRecord(String recordId);

    List<MedicalStaffFeedbackResponse> getRelatedStaffsForFeedback(String recordId);

    List<MedicalServiceForFeedbackResponse> getRelatedServicesForFeedback(String recordId);

    MedicalResponse addServicesAsNewInvoice(String recordId, InvoiceServiceRequest req);
}
