package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.service.StaffFeedbackService;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/staff-feedbacks")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class StaffFeedbackController {
    StaffFeedbackService staffFeedbackService;

    @PostMapping
    public ApiResponse<StaffFeedbackResponse> create(@RequestBody @Valid StaffFeedbackRequest request) {
        log.info("Request to create staff feedback: {}", request);
        StaffFeedbackResponse response = staffFeedbackService.create(request);
        return ApiResponse.<StaffFeedbackResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<StaffFeedbackResponse> update(@PathVariable String id, @RequestBody @Valid StaffFeedbackRequest request) {
        log.info("Request to update staff feedback with ID {}: {}", id, request);
        StaffFeedbackResponse response = staffFeedbackService.update(id, request);
        return ApiResponse.<StaffFeedbackResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping
    public ApiResponse<List<StaffFeedbackResponse>> getAll() {
        log.info("Request to get all staff feedbacks");
        List<StaffFeedbackResponse> responses = staffFeedbackService.findAll();
        return ApiResponse.<List<StaffFeedbackResponse>>builder()
                .result(responses)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<StaffFeedbackResponse> getById(@PathVariable String id) {
        log.info("Request to get staff feedback by ID: {}", id);
        StaffFeedbackResponse response = staffFeedbackService.findById(id);
        return ApiResponse.<StaffFeedbackResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        log.info("Request to delete staff feedback by ID: {}", id);
        staffFeedbackService.delete(id);
        return ApiResponse.<String>builder()
                .result("Feedback with ID " + id + " has been deleted successfully")
                .build();
    }

    @GetMapping("/by-record/{medicalRecordId}")
    public ApiResponse<List<StaffFeedbackResponse>> getByMedicalRecordId(@PathVariable String medicalRecordId) {
        log.info("Request to get staff feedbacks by medical record ID: {}", medicalRecordId);
        List<StaffFeedbackResponse> responses = staffFeedbackService.findByMedicalRecordId(medicalRecordId);
        return ApiResponse.<List<StaffFeedbackResponse>>builder()
                .result(responses)
                .build();
    }

}
