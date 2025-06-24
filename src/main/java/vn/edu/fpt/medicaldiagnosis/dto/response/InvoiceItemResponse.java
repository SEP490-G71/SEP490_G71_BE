package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceItemResponse {
    private String name;
    private String serviceCode;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal vat;
    private BigDecimal total;
}
