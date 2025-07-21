package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceItemReportItem {
    private String serviceCode;
    private String name;
    private BigDecimal price;
    private long totalUsage;      // Tổng quantity
    private BigDecimal totalOriginal;
    private BigDecimal totalDiscount;
    private BigDecimal totalVat;
    private BigDecimal totalRevenue; // Tổng total
}
