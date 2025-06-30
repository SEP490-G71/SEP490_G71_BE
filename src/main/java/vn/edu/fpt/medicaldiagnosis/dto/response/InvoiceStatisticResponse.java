package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceStatisticResponse {
    private PagedResponse<InvoiceResponse> data;
    private long totalInvoices;
    private BigDecimal totalAmount;
    private BigDecimal monthlyRevenue;
    private long validInvoices;
}
