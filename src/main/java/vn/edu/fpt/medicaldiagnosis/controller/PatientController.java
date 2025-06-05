package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;

import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/patients")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class PatientController {

    PatientService patientService;

    @PostMapping
    public ApiResponse<PatientResponse> createPatient(@RequestBody @Valid PatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.createPatient(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PatientResponse>> getAllPatients() {
        return ApiResponse.<List<PatientResponse>>builder()
                .result(patientService.getAllPatients())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientResponse> getPatientById(@PathVariable String id) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.getPatientById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PatientResponse> updatePatient(@PathVariable String id,
                                                      @RequestBody @Valid PatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.updatePatient(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePatient(@PathVariable String id) {
        patientService.deletePatient(id);
        return ApiResponse.<String>builder().message("Patient deleted successfully.").build();
    }
}
