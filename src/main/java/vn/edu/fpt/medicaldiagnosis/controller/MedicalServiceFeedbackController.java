package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.service.MedicalServiceFeedbackService;

import java.util.List;

@RestController
@RequestMapping("/medical-service-feedbacks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MedicalServiceFeedbackController {

    MedicalServiceFeedbackService medicalServiceFeedbackService;

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

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        log.info("Request to delete medical service feedback by ID: {}", id);
        medicalServiceFeedbackService.delete(id);
        return ApiResponse.<String>builder()
                .result("Feedback with ID " + id + " has been deleted successfully")
                .build();
    }
}

