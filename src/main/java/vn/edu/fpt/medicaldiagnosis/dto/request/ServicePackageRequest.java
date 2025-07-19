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

    @NotNull(message = "SERVICE_PACKAGE_QUANTITY_REQUIRED")
    @Min(value = 1, message = "SERVICE_PACKAGE_QUANTITY_MIN_1")
    private Integer quantity;
}
