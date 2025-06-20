package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QueuePatientsResponse {
    private String id;
    private String queueId;
    private String patientId;
    private Long queueOrder;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String departmentId;
}
