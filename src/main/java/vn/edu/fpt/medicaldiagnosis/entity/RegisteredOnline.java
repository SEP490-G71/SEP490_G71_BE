package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registered_online")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class RegisteredOnline extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "visit_count", nullable = false)
    private Integer visitCount = 1;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;
}
