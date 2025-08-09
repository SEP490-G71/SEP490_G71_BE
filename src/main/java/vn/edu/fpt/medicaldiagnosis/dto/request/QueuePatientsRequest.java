package vn.edu.fpt.medicaldiagnosis.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueuePatientsRequest {

    private String queueId;

    @NotNull(message = "PATIENT_ID_REQUIRED")
    private String patientId;

    private Long queueOrder;

    private String status;

    private LocalDateTime checkinTime;

    private LocalDateTime checkoutTime;

    private String roomNumber;

    @JsonProperty
    private DepartmentType type;

    private LocalDateTime calledTime;

    private Boolean isPriority;

    @NotNull(message = "REGISTERED_TIME_REQUIRED")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime registeredTime;

    @NotBlank(message = "SPECIALIZATION_ID_REQUIRED")
    private String specializationId;

    private LocalDateTime awaitingResultTime;

    private String receptionistId;

    private String message;
}
