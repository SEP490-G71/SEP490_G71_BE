package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.dto.request.LeaveRequestDetailDTO;
import vn.edu.fpt.medicaldiagnosis.enums.LeaveRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private String id;
    private String staffName;
    private String reason;
    private LeaveRequestStatus status;
    private LocalDateTime createdAt;
    private List<LeaveRequestDetailDTO> details;
}

