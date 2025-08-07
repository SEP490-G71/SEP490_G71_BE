package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceFeedbackResponse;

import java.util.List;

public interface MedicalServiceFeedbackService {
    MedicalServiceFeedbackResponse create(MedicalServiceFeedbackRequest request);
    MedicalServiceFeedbackResponse update(String id, MedicalServiceFeedbackRequest request);
    List<MedicalServiceFeedbackResponse> findAll();
    MedicalServiceFeedbackResponse findById(String id);
    void delete(String id);
}
