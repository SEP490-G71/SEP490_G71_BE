package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryRequest {

    @NotBlank(message = "TENANT_ID_REQUIRED")
    private String tenantId;

    @NotBlank(message = "SERVICE_PACKAGE_ID_REQUIRED")
    private String servicePackageId;

    @NotNull(message = "PRICE_REQUIRED")
    private Double price;

    @NotNull(message = "TRANSACTION_START_DATE_REQUIRED")
    private LocalDateTime startDate;

    @NotNull(message = "TRANSACTION_END_DATE_REQUIRED")
    private LocalDateTime endDate;
}
