package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.LeaveRequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeaveRequestStatusRequest {
    @NotBlank(message = "LEAVE_REQUEST_ID_REQUIRED")
    private String leaveRequestId;

    @NotNull(message = "LEAVE_REQUEST_STATUS_REQUIRED")
    private LeaveRequestStatus status;

    @Size(max = 255)
    private String note; // Lý do từ chối nếu cần
}
