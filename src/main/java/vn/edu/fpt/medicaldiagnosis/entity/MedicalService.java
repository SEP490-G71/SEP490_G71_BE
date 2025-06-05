package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "medical_services")
public class MedicalService extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne()
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(precision = 15, scale = 3, nullable = false)
    private BigDecimal price; // VND, không có phần thập phân

    @Column(precision = 5, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, scale = 1, precision = 3)
    private BigDecimal vat; // chỉ chấp nhận 8 hoặc 10
}
