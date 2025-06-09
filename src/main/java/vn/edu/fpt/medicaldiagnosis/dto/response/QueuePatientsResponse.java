package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueuePatientsResponse {
    private String id;
    private Long queueOrder;
    private String status;
    private String departmentId;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
}
