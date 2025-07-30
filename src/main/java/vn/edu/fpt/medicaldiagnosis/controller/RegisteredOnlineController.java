package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.RegisteredOnlineResponse;
import vn.edu.fpt.medicaldiagnosis.service.RegisteredOnlineService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/registered-online")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
public class RegisteredOnlineController {

    RegisteredOnlineService service;

    @PostMapping
    public ApiResponse<RegisteredOnlineResponse> create(@RequestBody @Valid RegisteredOnlineRequest request) {
        log.info("Registering appointment: {}", request);
        return ApiResponse.<RegisteredOnlineResponse>builder()
                .result(service.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<RegisteredOnlineResponse>> getAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<RegisteredOnlineResponse> result = service.getPaged(filters, page, size, sortBy, sortDir);

        return ApiResponse.<PagedResponse<RegisteredOnlineResponse>>builder()
                .result(new PagedResponse<>(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isLast()
                ))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RegisteredOnlineResponse> getById(@PathVariable String id) {
        return ApiResponse.<RegisteredOnlineResponse>builder()
                .result(service.getById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xoá lịch hẹn thành công")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<RegisteredOnlineResponse> update(
            @PathVariable String id,
            @RequestBody @Valid RegisteredOnlineRequest request
    ) {
        log.info("Updating appointment with id {}: {}", id, request);
        return ApiResponse.<RegisteredOnlineResponse>builder()
                .result(service.update(id, request))
                .build();
    }

    @PutMapping("/status/{id}")
    public ApiResponse<RegisteredOnlineResponse> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid RegisteredOnlineRequest request
    ) {
        log.info("Updating appointment with id {}: {}", id, request);
        return ApiResponse.<RegisteredOnlineResponse>builder()
                .result(service.updateStatus(id, request))
                .build();
    }
}
