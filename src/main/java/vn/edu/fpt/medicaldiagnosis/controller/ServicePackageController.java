package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.ServicePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.ServicePackageResponse;
import vn.edu.fpt.medicaldiagnosis.service.ServicePackageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/service-packages")
@RequiredArgsConstructor
@Slf4j
public class ServicePackageController {

    private final ServicePackageService service;

    @PostMapping
    ApiResponse<ServicePackageResponse> create(@Valid @RequestBody ServicePackageRequest request) {
        ServicePackageResponse response = service.create(request);
        return ApiResponse.<ServicePackageResponse>builder().result(response).build();
    }

    @PutMapping("/{id}")
    ApiResponse<ServicePackageResponse> update(@PathVariable String id,
                                               @Valid @RequestBody ServicePackageRequest request) {
        ServicePackageResponse response = service.update(id, request);
        return ApiResponse.<ServicePackageResponse>builder().result(response).build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.<String>builder().result("Deleted successfully").build();
    }

    @GetMapping("/{id}")
    ApiResponse<ServicePackageResponse> getById(@PathVariable String id) {
        ServicePackageResponse response = service.getById(id);
        return ApiResponse.<ServicePackageResponse>builder().result(response).build();
    }

    @GetMapping
    ApiResponse<List<ServicePackageResponse>> getAll() {
        List<ServicePackageResponse> response = service.getAll();
        return ApiResponse.<List<ServicePackageResponse>>builder().result(response).build();
    }

    @GetMapping("/search")
    ApiResponse<PagedResponse<ServicePackageResponse>> getPaged(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<ServicePackageResponse> result = service.getServicePackagesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<ServicePackageResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<ServicePackageResponse>>builder().result(response).build();
    }
}
