package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePackageRequest {
    @NotBlank(message = "TENANT_CODE_REQUIRED")
    private String tenantCode;

    @NotBlank(message = "SERVICE_PACKAGE_ID_REQUIRED")
    private String packageId;
}
