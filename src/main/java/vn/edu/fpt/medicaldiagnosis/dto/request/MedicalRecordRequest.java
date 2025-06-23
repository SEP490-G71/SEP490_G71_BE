package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordRequest {

    @NotBlank(message = "Patient ID must not be blank")
    private String patientId;

    @NotBlank(message = "Staff ID must not be blank")
    private String staffId;

    @NotBlank(message = "Diagnosis text must not be blank")
    private String diagnosisText;

    @NotEmpty(message = "Service list must not be empty")
    @Valid
    private List<@Valid ServiceRequest> services;

    @Data
    public static class ServiceRequest {

        @NotBlank(message = "Service ID must not be blank")
        private String serviceId;

        @NotNull(message = "Quantity must not be null")
        private Integer quantity;
    }
}
