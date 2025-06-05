package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    PatientResponse createPatient(PatientRequest request);
    List<PatientResponse> getAllPatients();
    PatientResponse getPatientById(String id);
    void deletePatient(String id);
    PatientResponse updatePatient(String id, PatientRequest request);
}
