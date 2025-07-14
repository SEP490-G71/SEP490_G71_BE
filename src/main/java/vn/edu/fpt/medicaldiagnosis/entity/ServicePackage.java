package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Table(name = "service_packages")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class ServicePackage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "description")
    private String description;

    /**
     * Kiểu thanh toán: WEEKLY, MONTHLY, YEARLY
     */
    @Column(name = "billing_type", nullable = false)
    private String billingType;

    @Column(name = "price", nullable = false)
    private Double price;

    /**
     * Trạng thái: ACTIVE, EXPIRED, CANCELLED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}
