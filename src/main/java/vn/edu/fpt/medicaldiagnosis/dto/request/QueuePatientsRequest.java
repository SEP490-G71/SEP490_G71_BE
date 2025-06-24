package vn.edu.fpt.medicaldiagnosis.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;

@Data
@Builder
public class QueuePatientsRequest {
    private String queueId;
    private String patientId;
    private Long queueOrder;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String roomNumber;
    @JsonProperty
    private DepartmentType type;
}
