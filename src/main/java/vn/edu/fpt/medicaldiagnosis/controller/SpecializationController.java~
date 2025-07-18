package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.SpecializationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.SpecializationResponse;
import vn.edu.fpt.medicaldiagnosis.service.SpecializationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/specializations")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpecializationController {

    SpecializationService specializationService;

    @PostMapping
    public ApiResponse<SpecializationResponse> createSpecialization(@RequestBody @Valid SpecializationRequest request) {
        log.info("Creating specialization: {}", request);
        SpecializationResponse result = specializationService.createSpecialization(request);
        return ApiResponse.<SpecializationResponse>builder().result(result).build();
    }

    @GetMapping("/all")
    public ApiResponse<List<SpecializationResponse>> getAllSpecializations() {
        log.info("Fetching all specializations");
        List<SpecializationResponse> result = specializationService.getAllSpecializations();
        return ApiResponse.<List<SpecializationResponse>>builder().result(result).build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<SpecializationResponse>> getSpecializations(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<SpecializationResponse> result = specializationService.getSpecializationsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<SpecializationResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<SpecializationResponse>>builder().result(response).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SpecializationResponse> getSpecializationById(@PathVariable String id) {
        log.info("Fetching specialization by id: {}", id);
        return ApiResponse.<SpecializationResponse>builder()
                .result(specializationService.getSpecializationById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SpecializationResponse> updateSpecialization(
            @PathVariable String id,
            @RequestBody @Valid SpecializationRequest request) {

        log.info("Updating specialization with id: {}", id);
        return ApiResponse.<SpecializationResponse>builder()
                .result(specializationService.updateSpecialization(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteSpecialization(@PathVariable String id) {
        log.info("Deleting specialization with id: {}", id);
        specializationService.deleteSpecialization(id);
        return ApiResponse.<String>builder()
                .message("Specialization deleted successfully.")
                .build();
    }
}
