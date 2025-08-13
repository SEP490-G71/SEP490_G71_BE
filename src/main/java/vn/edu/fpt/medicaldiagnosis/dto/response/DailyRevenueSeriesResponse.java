package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueSeriesResponse {
    private YearMonth month;
    private BigDecimal monthlyTarget;
    private List<DailyRevenuePoint> data;

    // Lũy kế tới hôm nay
    private BigDecimal totalToDate;       // Σ revenue đến hôm nay
    private BigDecimal expectedToDate;    // Σ expected đến hôm nay
    private BigDecimal diffPctToDate;     // (totalToDate - expectedToDate)/expectedToDate*100
    private String levelToDate;           // OK | WARN | CRITICAL
}
