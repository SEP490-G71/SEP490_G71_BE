package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResponseDTO;
import vn.edu.fpt.medicaldiagnosis.service.MedicalRecordService;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/medical-record")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MedicalRecordController {
    MedicalRecordService medicalRecordService;

    @PostMapping
    public ApiResponse<MedicalResponseDTO> createMedicalRecord(@RequestBody @Valid MedicalRequestDTO request) {
        log.info("Controller: Creating medical record with request: {}", request);
        ApiResponse<MedicalResponseDTO> response = new ApiResponse<>();
        response.setResult(medicalRecordService.createMedicalRecord(request));
        return response;
    }

    @GetMapping("/{recordId}")
    public ApiResponse<MedicalRecordResponse> getMedicalRecordDetail(@PathVariable String recordId) {
        return ApiResponse.<MedicalRecordResponse>builder()
                .message("Get medical record detail successfully")
                .result(medicalRecordService.getMedicalRecordDetail(recordId))
                .build();
    }

}
