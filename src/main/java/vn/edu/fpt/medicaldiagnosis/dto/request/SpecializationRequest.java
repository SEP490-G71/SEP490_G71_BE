package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecializationRequest {
    @NotBlank(message = "SPECIALIZATION_NAME_REQUIRED")
    private String name;
    private String description;
}
