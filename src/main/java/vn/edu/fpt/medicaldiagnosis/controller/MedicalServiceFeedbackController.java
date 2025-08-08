package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceFeedbackStatisticResponse;
import vn.edu.fpt.medicaldiagnosis.service.MedicalServiceFeedbackService;
import vn.edu.fpt.medicaldiagnosis.service.impl.ExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/medical-service-feedbacks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MedicalServiceFeedbackController {

    MedicalServiceFeedbackService medicalServiceFeedbackService;
    ExportServiceImpl exportService;
    @PostMapping
    public ApiResponse<MedicalServiceFeedbackResponse> create(@RequestBody @Valid MedicalServiceFeedbackRequest request) {
        log.info("Request to create medical service feedback: {}", request);
        return ApiResponse.<MedicalServiceFeedbackResponse>builder()
                .result(medicalServiceFeedbackService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicalServiceFeedbackResponse> update(@PathVariable String id, @RequestBody @Valid MedicalServiceFeedbackRequest request) {
        log.info("Request to update medical service feedback with ID {}: {}", id, request);
        return ApiResponse.<MedicalServiceFeedbackResponse>builder()
                .result(medicalServiceFeedbackService.update(id, request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<MedicalServiceFeedbackResponse>> getAll() {
        log.info("Request to get all medical service feedbacks");
        return ApiResponse.<List<MedicalServiceFeedbackResponse>>builder()
                .result(medicalServiceFeedbackService.findAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalServiceFeedbackResponse> getById(@PathVariable String id) {
        log.info("Request to get medical service feedback by ID: {}", id);
        return ApiResponse.<MedicalServiceFeedbackResponse>builder()
                .result(medicalServiceFeedbackService.findById(id))
                .build();
    }

    @GetMapping("/by-medical-service/{medicalServiceId}")
    public ApiResponse<List<MedicalServiceFeedbackResponse>> getByMedicalServiceId(@PathVariable String medicalServiceId) {
        log.info("Request to get feedbacks by medical service ID: {}", medicalServiceId);
        List<MedicalServiceFeedbackResponse> responses = medicalServiceFeedbackService.findByMedicalServiceId(medicalServiceId);
        return ApiResponse.<List<MedicalServiceFeedbackResponse>>builder()
                .result(responses)
                .build();
    }


    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        log.info("Request to delete medical service feedback by ID: {}", id);
        medicalServiceFeedbackService.delete(id);
        return ApiResponse.<String>builder()
                .result("Feedback with ID " + id + " has been deleted successfully")
                .build();
    }

    @GetMapping("/by-record/{medicalRecordId}")
    public ApiResponse<List<MedicalServiceFeedbackResponse>> getByMedicalRecordId(@PathVariable String medicalRecordId) {
        log.info("Request to get medical service feedbacks by medical record ID: {}", medicalRecordId);
        return ApiResponse.<List<MedicalServiceFeedbackResponse>>builder()
                .result(medicalServiceFeedbackService.findByMedicalRecordId(medicalRecordId))
                .build();
    }

    @GetMapping("/feedback-statistics")
    public ApiResponse<MedicalServiceFeedbackStatisticResponse> getServiceFeedbackStatistics(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ApiResponse.<MedicalServiceFeedbackStatisticResponse>builder()
                .result(medicalServiceFeedbackService.getServiceFeedbackStatistics(filters, page, size, sortBy, sortDir))
                .build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMedicalServiceFeedbackStatisticsExcel(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) throws IOException {
        MedicalServiceFeedbackStatisticResponse stats = medicalServiceFeedbackService
                .getServiceFeedbackStatistics(filters, 0, Integer.MAX_VALUE, sortBy, sortDir);

        ByteArrayInputStream in = exportService.exportMedicalServiceFeedbackStatisticsToExcel(
                stats.getData().getContent(),
                stats.getTotalFeedbacks(),
                stats.getAverageSatisfaction()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=medical_service_feedbacks.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }
}

