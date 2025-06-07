package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.Level;
import vn.edu.fpt.medicaldiagnosis.enums.Specialty;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "staffs")
public class Staff extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Specialty specialty;

    @Enumerated(EnumType.STRING)
    private Level level;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dob;

    @Column(name = "account_id")
    private String accountId;
}
