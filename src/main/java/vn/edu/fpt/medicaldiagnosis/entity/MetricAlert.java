package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.AlertLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "metric_alerts")
public class MetricAlert extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "metric_code", nullable = false)
    private String metricCode; // vd: dailyRevenue

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private AlertLevel level;

    @Column(name = "actual_value", precision = 18, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "target_value", precision = 18, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "diff_pct", precision = 7, scale = 2)
    private BigDecimal diffPct;

    @Column(name = "mom_pct", precision = 7, scale = 2)
    private BigDecimal momPct;

    private String reason;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson; // JSON từ AI
}
