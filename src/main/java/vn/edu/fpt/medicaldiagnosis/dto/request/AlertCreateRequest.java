package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.AlertLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertCreateRequest {
    private String metricCode;            // vd: dailyRevenue
    private LocalDate periodStart;        // kỳ bắt đầu
    private LocalDate periodEnd;          // kỳ kết thúc
    private AlertLevel level;              // OK / WARN / CRITICAL
    private BigDecimal actualValue;        // giá trị thực tế
    private BigDecimal targetValue;        // giá trị mục tiêu
    private BigDecimal diffPct;            // % lệch so với target
    private BigDecimal momPct;             // % thay đổi so với hôm trước/tháng trước
    private String reason;                 // lý do ngắn gọn
    private String payloadJson;            // dữ liệu JSON từ AI
}
