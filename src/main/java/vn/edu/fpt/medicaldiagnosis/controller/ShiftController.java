package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.ShiftRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.ShiftResponse;
import vn.edu.fpt.medicaldiagnosis.service.ShiftService;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/shifts")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ShiftController {
    ShiftService shiftService;

    @PostMapping
    public ApiResponse<ShiftResponse> createShift(@RequestBody @Valid ShiftRequest request) {
        log.info("Controller: create shift with data: {}", request);
        return ApiResponse.<ShiftResponse>builder()
                .result(shiftService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<ShiftResponse>> getShifts(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get shifts with filters {}", filters);

        Page<ShiftResponse> result = shiftService.getShiftsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<ShiftResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<ShiftResponse>>builder().result(response).build();
    }

    @GetMapping("/all")
    public ApiResponse<List<ShiftResponse>> getAllShifts() {
        log.info("Controller: get all shifts");
        return ApiResponse.<List<ShiftResponse>>builder()
                .result(shiftService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ShiftResponse> getShiftById(@PathVariable String id) {
        log.info("Controller: get shift by id {}", id);
        return ApiResponse.<ShiftResponse>builder()
                .result(shiftService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ShiftResponse> updateShift(@PathVariable String id, @RequestBody @Valid ShiftRequest request) {
        log.info("Controller: update shift {}", id);
        return ApiResponse.<ShiftResponse>builder()
                .result(shiftService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteShift(@PathVariable String id) {
        log.info("Controller: delete shift {}", id);
        shiftService.delete(id);
        return ApiResponse.<String>builder()
                .message("Shift deleted successfully.")
                .build();
    }
}
