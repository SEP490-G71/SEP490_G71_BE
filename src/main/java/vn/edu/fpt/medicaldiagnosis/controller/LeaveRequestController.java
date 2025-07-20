package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequestByTime;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.service.LeaveRequestService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/leave-requests")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class LeaveRequestController {
    LeaveRequestService leaveRequestService;

    @PostMapping
    public ApiResponse<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequest request) {
        log.info("Controller: create leave request");
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        return ApiResponse.<LeaveRequestResponse>builder()
                .code(1000)
                .message("Tạo đơn xin nghỉ thành công")
                .result(response)
                .build();
    }

    @PutMapping("/status")
    public ApiResponse<LeaveRequestResponse> updateLeaveRequestStatus(
            @Valid @RequestBody UpdateLeaveRequestStatusRequest request) {
        log.info("Controller: update leave request status");
        LeaveRequestResponse response = leaveRequestService.updateLeaveRequestStatus(request);
        return ApiResponse.<LeaveRequestResponse>builder()
                .code(1000)
                .message("Cập nhật trạng thái đơn xin nghỉ thành công")
                .result(response)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLeaveRequest(@PathVariable("id") String id) {
        log.info("Controller: delete leave request {}", id);
        leaveRequestService.deleteLeaveRequest(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đơn xin nghỉ thành công")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<LeaveRequestResponse> updateLeaveRequest(
            @PathVariable("id") String id,
            @Valid @RequestBody CreateLeaveRequest request) {

        LeaveRequestResponse response = leaveRequestService.updateLeaveRequest(id, request);
        return ApiResponse.<LeaveRequestResponse>builder()
                .code(1000)
                .message("Cập nhật đơn xin nghỉ thành công")
                .result(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<LeaveRequestResponse>> getLeaveRequests(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Controller: get leave requests with filters={}, page={}, size={}, sortBy={}, sortDir={}", filters, page, size, sortBy, sortDir);
        Page<LeaveRequestResponse> result = leaveRequestService.getLeaveRequestsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<LeaveRequestResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<LeaveRequestResponse>>builder().result(response).build();
    }

    @GetMapping("/my-requests")
    public ApiResponse<PagedResponse<LeaveRequestResponse>> getMyLeaveRequests(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestAttribute("staffId") String staffId // hoặc lấy từ token đã xác thực
    ) {
        // Gán staffId vào filters để lọc đúng người dùng
        filters.put("staffId", staffId);

        Page<LeaveRequestResponse> result = leaveRequestService.getLeaveRequestsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<LeaveRequestResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<LeaveRequestResponse>>builder()
                .result(response)
                .code(1000)
                .message("Lấy danh sách đơn xin nghỉ của bạn thành công")
                .build();
    }

    @GetMapping("/staff/{staffId}")
    public ApiResponse<PagedResponse<LeaveRequestResponse>> getLeaveRequestsByStaffId(
            @PathVariable String staffId,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Controller: get leave requests of staff {}, filters={}", staffId, filters);

        filters.put("staffId", staffId); // Gán staffId vào filter để tái sử dụng logic chung
        Page<LeaveRequestResponse> result = leaveRequestService.getLeaveRequestsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<LeaveRequestResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<LeaveRequestResponse>>builder().result(response).build();
    }

    @PostMapping("/by-time")
    public ApiResponse<LeaveRequestResponse> createLeaveRequestByTime(@Valid @RequestBody CreateLeaveRequestByTime request) {
        log.info("Controller: create leave request");
        LeaveRequestResponse response = leaveRequestService.createLeaveRequestByTime(request);
        return ApiResponse.<LeaveRequestResponse>builder()
                .code(1000)
                .message("Tạo đơn xin nghỉ thành công")
                .result(response)
                .build();
    }
}
