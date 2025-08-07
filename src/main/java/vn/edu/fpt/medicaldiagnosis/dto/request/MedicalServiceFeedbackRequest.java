package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.SatisfactionLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalServiceFeedbackRequest {
    @NotNull
    private String medicalServiceId;

    @NotNull
    private String patientId;

    @NotNull
    private String medicalRecordId;

    @NotNull
    private SatisfactionLevel satisfactionLevel;

    private String comment;
}
