package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponse {
    private String id;
    private String tenantId;
    private String servicePackageId;
    private Double price;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
