package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.BulkUpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.WorkScheduleService;
import vn.edu.fpt.medicaldiagnosis.service.impl.ExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/work-schedules")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class WorkScheduleController {
    WorkScheduleService workScheduleService;
    ExportServiceImpl exportService;
    @PostMapping
    public ApiResponse<WorkScheduleRecurringResponse> createRecurringSchedules(
            @RequestBody @Valid WorkScheduleRecurringRequest request
    ) {
        log.info("Controller: create recurring schedules - {}", request);

        WorkScheduleRecurringResponse result = workScheduleService.createRecurringSchedules(request);

        return ApiResponse.<WorkScheduleRecurringResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/check-in/{id}")
    public ApiResponse<WorkScheduleCreateResponse> checkIn(@PathVariable("id") String id) {
        log.info("Controller: check-in work schedule {}", id);
        WorkScheduleCreateResponse result = workScheduleService.checkIn(id);
        return ApiResponse.<WorkScheduleCreateResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/staff/{staffId}")
    public ApiResponse<List<WorkScheduleDetailResponse>> getAllSchedulesByStaff(
            @PathVariable String staffId
    ) {
        log.info("Get all schedules for staff: {}", staffId);
        List<WorkScheduleDetailResponse> result = workScheduleService.getAllSchedulesByStaffId(staffId);
        return ApiResponse.<List<WorkScheduleDetailResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("")
    public ApiResponse<PagedResponse<WorkScheduleRecurringResponse>> getRecurringSchedules(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "shiftDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Controller: get recurring schedules with filters={}, page={}, size={}, sortBy={}, sortDir={}", filters, page, size, sortBy, sortDir);
        Page<WorkScheduleRecurringResponse> result =
                workScheduleService.getRecurringSchedulesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<WorkScheduleRecurringResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<WorkScheduleRecurringResponse>>builder().result(response).build();
    }

    @GetMapping("/staff/{staffId}/detail")
    public ApiResponse<WorkScheduleRecurringResponse> getScheduleDetailByStaff(
            @PathVariable String staffId) {
        WorkScheduleRecurringResponse response = workScheduleService.getRecurringScheduleDetailByStaffId(staffId);
        return ApiResponse.<WorkScheduleRecurringResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("")
    public ApiResponse<WorkScheduleRecurringResponse> updateRecurringSchedule(
            @Valid @RequestBody WorkScheduleRecurringRequest request) {

        WorkScheduleRecurringResponse response = workScheduleService.updateRecurringSchedules(request);
        return ApiResponse.<WorkScheduleRecurringResponse>builder()
                .result(response)
                .code(1000)
                .message("Cập nhật lịch làm việc định kỳ thành công")
                .build();
    }

    @PutMapping("/update-detail/{id}")
    public ApiResponse<WorkScheduleDetailResponse> updateWorkSchedule(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateWorkScheduleRequest request) {

        WorkScheduleDetailResponse response = workScheduleService.updateWorkSchedule(id, request);
        return ApiResponse.<WorkScheduleDetailResponse>builder()
                .result(response)
                .message("Cập nhật buổi làm việc thành công")
                .code(1000)
                .build();
    }

    @PutMapping("/bulk-update/{staffId}")
    public ApiResponse<List<WorkScheduleDetailResponse>> updateMultipleWorkSchedule(
            @PathVariable("staffId") String staffId,
            @Valid @RequestBody BulkUpdateWorkScheduleRequest request) {

        List<WorkScheduleDetailResponse> response = workScheduleService.bulkUpdateWorkSchedules(staffId, request);
        return ApiResponse.<List<WorkScheduleDetailResponse>>builder()
                .result(response)
                .message("Cập nhật lịch làm việc thành công")
                .code(1000)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkSchedule(@PathVariable String id) {
        log.info("Controller: delete work schedule {}", id);
        workScheduleService.deleteWorkSchedule(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa buổi làm việc thành công")
                .build();
    }

    @DeleteMapping("/by-staff/{staffId}")
    public ApiResponse<Void> deleteWorkSchedulesByStaffId(@PathVariable String staffId) {
        log.info("Controller: delete work schedules for staff {}", staffId);
        workScheduleService.deleteWorkSchedulesByStaffId(staffId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa lịch làm việc thành công")
                .build();
    }

    @GetMapping("/statistics")
    public ApiResponse<WorkScheduleStatisticResponse> getWorkScheduleStatistics(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "staffName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Controller: get work schedule statistics with filters={}, page={}, size={}, sortBy={}, sortDir={}", filters, page, size, sortBy, sortDir);
        return ApiResponse.<WorkScheduleStatisticResponse>builder()
                .result(workScheduleService.getWorkScheduleStatistics(filters, page, size, sortBy, sortDir))
                .build();
    }

    @GetMapping("/statistics/export")
    public ResponseEntity<byte[]> exportWorkScheduleStatisticsExcel(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(defaultValue = "staffName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) throws IOException {
        WorkScheduleStatisticResponse data = workScheduleService.getWorkScheduleStatistics(filters, page, size, sortBy, sortDir);
        ByteArrayInputStream in = exportService.exportWorkScheduleToExcel(data.getDetails().getContent(), data);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=work_schedule_statistics.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }
}
