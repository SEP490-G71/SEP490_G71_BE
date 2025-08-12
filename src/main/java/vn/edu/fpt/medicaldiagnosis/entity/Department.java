package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "departments")
public class Department extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String description;
    @Column(name = "room_number")
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private DepartmentType type;

    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @Column(name = "is_overloaded", nullable = false)
    private boolean overloaded;
}
