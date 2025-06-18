package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private String invoiceId;
    private BigDecimal amount;
    private String paymentType;
    private InvoiceStatus status;            // "PAID" | "CANCELLED" | ...
    private Instant paidAt;
}
