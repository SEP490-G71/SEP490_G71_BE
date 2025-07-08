package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceItemStatisticResponse {
    private long totalServiceTypes;
    private long totalUsage;
    private BigDecimal totalRevenue;
    private InvoiceItemReportItem mostUsedService;
    private PagedResponse<InvoiceItemReportItem> details;
}

