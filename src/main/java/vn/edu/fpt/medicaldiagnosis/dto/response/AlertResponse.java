package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.AlertLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private String id;
    private String metricCode;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private AlertLevel level;
    private BigDecimal actualValue;
    private BigDecimal targetValue;
    private BigDecimal diffPct;
    private BigDecimal momPct;
    private String reason;
    private String payloadJson;          // có thể parse ra object nếu muốn
}

