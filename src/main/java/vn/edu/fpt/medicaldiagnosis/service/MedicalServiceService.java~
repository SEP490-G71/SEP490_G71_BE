package vn.edu.fpt.medicaldiagnosis.service;


import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceResponse;

import java.util.List;
import java.util.Map;

public interface MedicalServiceService {
    MedicalServiceResponse createMedicalService(MedicalServiceRequest medicalServiceRequest);
    List<MedicalServiceResponse> getAllMedicalServices();

    MedicalServiceResponse getMedicalServiceById(String id);

    void deleteMedicalService(String id);

    MedicalServiceResponse updateMedicalService(String id, MedicalServiceRequest medicalServiceRequest);

    Page<MedicalServiceResponse> getMedicalServicesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
