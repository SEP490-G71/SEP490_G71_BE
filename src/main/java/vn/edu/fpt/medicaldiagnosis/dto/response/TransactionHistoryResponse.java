package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class TransactionHistoryResponse {
    String id;
    String tenantId;
    String tenantCode;
    String servicePackageId;

    String packageName;
    String billingType;
    Integer quantity;
    Integer price;

    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime createdAt;
}
