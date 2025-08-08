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
public class ServiceFeedBackResponse {
    private String id;
    private String serviceCode;

    private String name;

    private String description;

    private DepartmentResponse department;

    private long totalFeedbacks;

    private BigDecimal averageSatisfaction;
}
