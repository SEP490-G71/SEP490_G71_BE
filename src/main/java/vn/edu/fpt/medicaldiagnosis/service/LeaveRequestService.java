package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequestByTime;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;

import java.util.Map;

public interface LeaveRequestService {
    LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request);

    LeaveRequestResponse updateLeaveRequestStatus(UpdateLeaveRequestStatusRequest request);

    void deleteLeaveRequest(String leaveRequestId);

    LeaveRequestResponse updateLeaveRequest(String leaveRequestId, CreateLeaveRequest request);

    Page<LeaveRequestResponse> getLeaveRequestsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    LeaveRequestResponse createLeaveRequestByTime(CreateLeaveRequestByTime request);
}
