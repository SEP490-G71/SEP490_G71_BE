package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;
import vn.edu.fpt.medicaldiagnosis.service.LeaveRequestService;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/leave-request")
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

}
