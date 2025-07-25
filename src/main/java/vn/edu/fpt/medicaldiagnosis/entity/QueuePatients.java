package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class QueuePatients extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "queue_id", nullable = false)
    private String queueId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "queue_order")
    private Long queueOrder;

    @Column(name = "status") // WAITING, IN_ROOM, DONE
    private String status;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Column(name = "room_number")
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DepartmentType type;

    @Column(name = "called_time")
    private LocalDateTime calledTime;

    @Column(name = "is_priority")
    private Boolean isPriority;

    @Column(name = "registered_time")
    private LocalDateTime registeredTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;
}
