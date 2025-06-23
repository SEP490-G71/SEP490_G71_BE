package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;

import java.util.List;
import java.util.Map;
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

    @GetMapping("/all")
    public ApiResponse<List<PatientResponse>> getAllPatients() {
        return ApiResponse.<List<PatientResponse>>builder()
                .result(patientService.getAllPatients())
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<PatientResponse>> getPatients(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get patients with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<PatientResponse> result = patientService.getPatientsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<PatientResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<PatientResponse>>builder().result(response).build();
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
