package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.SpecializationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.SpecializationResponse;

import java.util.List;
import java.util.Map;

public interface SpecializationService {
    SpecializationResponse createSpecialization(SpecializationRequest request);
    SpecializationResponse updateSpecialization(String id, SpecializationRequest request);
    void deleteSpecialization(String id);
    List<SpecializationResponse> getAllSpecializations();
    Page<SpecializationResponse> getSpecializationsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
    SpecializationResponse getSpecializationById(String id);
}
