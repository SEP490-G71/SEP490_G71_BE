package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenuePoint {
    private LocalDate date;          // Ngày
    private BigDecimal revenue;      // Doanh thu thực tế ngày đó
    private BigDecimal expected;     // Baseline mỗi ngày = target / daysInMonth
    private BigDecimal diffPct;      // (revenue - expected)/expected*100
    private String level;            // OK | WARN | CRITICAL (theo ngày)
    private String direction;  // "UP" | "DOWN" | "FLAT"
    private String reason;     // "Above expected" | "Below expected"
}
