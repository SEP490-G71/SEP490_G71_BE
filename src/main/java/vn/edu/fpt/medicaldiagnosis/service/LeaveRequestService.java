package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;

public interface LeaveRequestService {
    LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request);

    LeaveRequestResponse updateLeaveRequestStatus(UpdateLeaveRequestStatusRequest request);

    void deleteLeaveRequest(String leaveRequestId);

    LeaveRequestResponse updateLeaveRequest(String leaveRequestId, CreateLeaveRequest request);
}
