package vn.edu.fpt.medicaldiagnosis.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;

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

    @Column(name = "medical_record_code", unique = true, nullable = false)
    private String medicalRecordCode;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MedicalRecordStatus status;
}
