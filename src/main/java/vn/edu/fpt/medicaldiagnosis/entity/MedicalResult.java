package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "medical_results")
public class MedicalResult extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "medical_order_id", nullable = false)
    private MedicalOrder medicalOrder;

    private String description;

    @Column(name = "result_note")
    private String resultNote;

    @ManyToOne
    @JoinColumn(name = "completed_by", nullable = false)
    private Staff completedBy;
}
