package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.StaffFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffFeedbackStatisticResponse;

import java.util.List;
import java.util.Map;

public interface StaffFeedbackService {
    StaffFeedbackResponse create(StaffFeedbackRequest request);
    StaffFeedbackResponse update(String id, StaffFeedbackRequest request);
    List<StaffFeedbackResponse> findAll();
    StaffFeedbackResponse findById(String id);
    void delete(String id);

    List<StaffFeedbackResponse> findByMedicalRecordId(String medicalRecordId);

    StaffFeedbackStatisticResponse getStaffFeedbackStatistics(
            Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    List<StaffFeedbackResponse> findByStaffId(String staffId);

}
