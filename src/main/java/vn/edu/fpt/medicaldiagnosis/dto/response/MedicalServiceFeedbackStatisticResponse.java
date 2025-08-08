package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalServiceFeedbackStatisticResponse {
    private PagedResponse<ServiceFeedBackResponse> data;
    private long totalFeedbacks;
    private BigDecimal averageSatisfaction;
}
