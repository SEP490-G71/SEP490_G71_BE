package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_transfer_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class RoomTransferHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Gắn với một bệnh án
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    // Phòng chuyển từ
    @ManyToOne()
    @JoinColumn(name="from_department_id", nullable=false)
    private Department fromDepartment;

    @ManyToOne()
    @JoinColumn(name="to_department_id", nullable=false)
    private Department toDepartment;

    // Ai thực hiện chuyển
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_by", nullable = false)
    private Staff transferredBy;

    // Thời điểm chuyển
    @Column(name = "transfer_time", nullable = false)
    private LocalDateTime transferTime;

    // Lý do (nếu có)
    @Column(name = "reason")
    private String reason;

    // Bác sĩ phụ trách ở phòng này
    @ManyToOne()
    @JoinColumn(name = "doctor_id")
    private Staff doctor;

    // Kết luận tại phòng này
    @Column(name = "conclusion_text", columnDefinition = "TEXT")
    private String conclusionText;

    // Có phải kết luận cuối cùng không
    @Column(name = "is_final")
    private Boolean isFinal;
}
