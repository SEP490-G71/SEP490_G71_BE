package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
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
    public ApiResponse<MedicalRecordResponse> createMedicalRecord(@RequestBody @Valid MedicalRecordRequest request) {
        log.info("Controller: Creating medical record with request: {}", request);
        ApiResponse<MedicalRecordResponse> response = new ApiResponse<>();
        response.setResult(medicalRecordService.createMedicalRecord(request));
        return response;
    }

}
