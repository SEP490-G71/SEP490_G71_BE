package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRequest {
    @NotBlank(message = "TENANT_NAME_REQUIRED")
    private String name;

    @NotBlank(message = "TENANT_CODE_REQUIRED")
    private String code;

    @NotBlank(message = "TENANT_EMAIL_REQUIRED")
    @Email(message = "TENANT_EMAIL_INVALID")
    private String email;

    @NotBlank(message = "TENANT_PHONE_REQUIRED")
    @Pattern(regexp = "\\d{10}", message = "TENANT_PHONE_INVALID")
    private String phone;

    @NotBlank(message = "TENANT_SERVICE_PACKAGE_REQUIRED")
    private String servicePackageId;
}
