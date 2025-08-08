package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_transfer_history")
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
    @Column(name = "from_room_number", nullable = false)
    private String fromRoomNumber;

    // Phòng chuyển đến
    @Column(name = "to_room_number", nullable = false)
    private String toRoomNumber;

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
}
