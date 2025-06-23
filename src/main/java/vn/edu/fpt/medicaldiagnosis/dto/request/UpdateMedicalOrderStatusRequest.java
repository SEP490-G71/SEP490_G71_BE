package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMedicalOrderStatusRequest {
    @NotBlank(message = "MEDICAL_ORDER_ID_EMPTY")
    private String medicalOrderId;

    @NotNull(message = "STATUS_INVALID")
    private MedicalOrderStatus status;
}
