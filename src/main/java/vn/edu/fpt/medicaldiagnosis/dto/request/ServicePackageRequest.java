package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.BillingType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePackageRequest {

    @NotBlank(message = "SERVICE_PACKAGE_TENANT_ID_REQUIRED")
    private String tenantId;

    @NotBlank(message = "SERVICE_PACKAGE_NAME_REQUIRED")
    @Size(min = 3, max = 100, message = "SERVICE_PACKAGE_NAME_LENGTH")
    private String packageName;

    @Size(max = 500, message = "SERVICE_PACKAGE_DESCRIPTION_LENGTH")
    private String description;

    @NotNull(message = "SERVICE_PACKAGE_BILLING_TYPE_REQUIRED")
    private BillingType billingType;

    @NotNull(message = "SERVICE_PACKAGE_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = true, message = "SERVICE_PACKAGE_PRICE_INVALID")
    private Double price;

    @NotNull(message = "SERVICE_PACKAGE_STATUS_REQUIRED")
    private Status status;

    @NotNull(message = "SERVICE_PACKAGE_START_DATE_REQUIRED")
    private LocalDateTime startDate;

    @NotNull(message = "SERVICE_PACKAGE_END_DATE_REQUIRED")
    private LocalDateTime endDate;
}
