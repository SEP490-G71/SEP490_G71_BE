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
public class MedicalServiceResponse {
    private String id;

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal discount;

    private BigDecimal vat;

    private String serviceCode;

    private DepartmentResponse department;

    private boolean defaultService;
}
