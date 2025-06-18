package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayInvoiceRequest {
    @NotBlank(message = "INVOICE_ID_EMPTY")
    private String invoiceId;

    @NotNull(message = "PAYMENT_TYPE_INVALID") // "CASH", "CARD", "TRANSFER" ...
    private PaymentType paymentType;
}
