package vn.edu.fpt.medicaldiagnosis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueuePatientsResponse {
    private String id;
    private String queueId;
    private String patientId;
    private Long queueOrder;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String roomNumber;
    private DepartmentType type;
    private LocalDateTime createdAt;
    private LocalDateTime calledTime;
    private Boolean isPriority;
    private LocalDateTime registeredTime;
    private LocalDateTime assignedTime;
    private String fullName;
}
