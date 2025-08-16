package vn.edu.fpt.medicaldiagnosis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private SpecializationResponse specialization;
    private LocalDateTime awaitingResultTime;
    private StaffResponse staff;
    private String message;
}
