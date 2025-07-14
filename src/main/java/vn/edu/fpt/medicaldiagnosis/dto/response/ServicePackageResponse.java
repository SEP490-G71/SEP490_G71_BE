package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePackageResponse {
    private String id;
    private String packageName;
    private String description;
    private String billingType;
    private Double price;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
