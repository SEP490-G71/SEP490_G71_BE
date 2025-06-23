package vn.edu.fpt.medicaldiagnosis.dto.request;


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
public class MedicalRequestDTO {

    @NotBlank(message = "PATIENT_ID_REQUIRED")
    private String patientId;

    @NotBlank(message = "STAFF_ID_REQUIRED")
    private String staffId;

    @NotBlank(message = "DIAGNOSIS_TEXT_REQUIRED")
    private String diagnosisText;

    @NotEmpty(message = "SERVICES_REQUIRED")
    private List<ServiceRequest> services;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceRequest {
        @NotBlank(message = "SERVICE_ID_REQUIRED")
        private String serviceId;

        @NotNull(message = "QUANTITY_REQUIRED")
        private Integer quantity;
    }
}
