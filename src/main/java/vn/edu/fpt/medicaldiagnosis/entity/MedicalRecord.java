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
@Table(name = "medical_records")
public class MedicalRecord extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

//    @ManyToOne
//    @JoinColumn(name = "visit_id", nullable = false)
//    private PatientVisit visit;

    @Column(name = "diagnosis_text")
    private String diagnosisText;

    @OneToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    private String summary;
}
