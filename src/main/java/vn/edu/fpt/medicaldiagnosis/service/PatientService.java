package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PatientService {
    PatientResponse createPatient(PatientRequest request);

    List<PatientResponse> getAllPatients();

    PatientResponse getPatientById(String id);

    void deletePatient(String id);

    PatientResponse updatePatient(String id, PatientRequest request);

    Page<PatientResponse> getPatientsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    Page<PatientResponse> getPatientsRegisteredTodayPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    List<PatientResponse> searchByNameOrCode(String keyword);
}
