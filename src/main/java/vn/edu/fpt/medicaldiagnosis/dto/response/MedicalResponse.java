package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalResponse {

    private String medicalRecordId;

    private String invoiceId;

    private BigDecimal totalAmount;
    private BigDecimal discountTotal;
    private BigDecimal vatTotal;
    private BigDecimal originalTotal;

    private List<String> medicalOrderIds;
}
