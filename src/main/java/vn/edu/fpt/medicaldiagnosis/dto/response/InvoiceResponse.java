package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private String invoiceId;
    private String invoiceCode;
    private String patientId;
    private String patientCode;
    private String patientName;
    private BigDecimal total;
    private BigDecimal discountTotal;
    private BigDecimal originalTotal;
    private BigDecimal vatTotal;
    private String paymentType;
    private String confirmedBy;
    private InvoiceStatus status;            // "PAID" | "CANCELLED" | ...
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
