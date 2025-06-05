package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.service.MedicalServiceService;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/medical-service")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MedicalServiceController {
    MedicalServiceService medicalService;

    @PostMapping
    ApiResponse<MedicalServiceResponse> createMedicalService(@RequestBody @Valid MedicalServiceRequest request) {
        log.info("Controller: {}", request);
        ApiResponse<MedicalServiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(medicalService.createMedicalService(request));
        return apiResponse;
    }

    @GetMapping
    public ApiResponse<PagedResponse<MedicalServiceResponse>> getDepartments(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<MedicalServiceResponse> result = medicalService.getMedicalServicesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<MedicalServiceResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<MedicalServiceResponse>>builder().result(response).build();
    }

    @GetMapping("/all")
    public ApiResponse<List<MedicalServiceResponse>> getAllMedicalServices() {
        log.info("Controller: get all medical services");
        return ApiResponse.<List<MedicalServiceResponse>>builder()
                .result(medicalService.getAllMedicalServices())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalServiceResponse> getMedicalServiceById(@PathVariable String id) {
        log.info("Controller: get medical service by id: {}", id);
        return ApiResponse.<MedicalServiceResponse>builder()
                .result(medicalService.getMedicalServiceById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteMedicalService(@PathVariable String id) {
        log.info("Controller: delete medical service {}", id);
        medicalService.deleteMedicalService(id);
        return ApiResponse.<String>builder()
                .message("Medical service deleted successfully.")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicalServiceResponse> updateMedicalService(@PathVariable String id, @RequestBody @Valid MedicalServiceRequest request) {
        log.info("Controller: update medical service {}", id);
        return ApiResponse.<MedicalServiceResponse>builder()
                .result(medicalService.updateMedicalService(id, request))
                .build();
    }
}
