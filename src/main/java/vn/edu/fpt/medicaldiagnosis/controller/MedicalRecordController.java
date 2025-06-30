package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.MedicalRecordService;

import java.util.Map;

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
    public ApiResponse<MedicalRecordDetailResponse> getMedicalRecordDetail(@PathVariable String recordId) {
        return ApiResponse.<MedicalRecordDetailResponse>builder()
                .message("Get medical record detail successfully")
                .result(medicalRecordService.getMedicalRecordDetail(recordId))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<MedicalRecordResponse>> getMedicalRecords(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Controller: get medical records with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<MedicalRecordResponse> result = medicalRecordService.getMedicalRecordsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<MedicalRecordResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
        return ApiResponse.<PagedResponse<MedicalRecordResponse>>builder().result(response).build();
    }
}
