package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.StaffFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffFeedbackResponse;

import java.util.List;

public interface StaffFeedbackService {
    StaffFeedbackResponse create(StaffFeedbackRequest request);
    StaffFeedbackResponse update(String id, StaffFeedbackRequest request);
    List<StaffFeedbackResponse> findAll();
    StaffFeedbackResponse findById(String id);
    void delete(String id);

    List<StaffFeedbackResponse> findByMedicalRecordId(String medicalRecordId);
}
