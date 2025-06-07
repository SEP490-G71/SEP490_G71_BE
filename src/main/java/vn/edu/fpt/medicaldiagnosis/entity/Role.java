package vn.edu.fpt.medicaldiagnosis.entity;

import java.util.Set;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.Where;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
@Table(name = "roles")
public class Role extends AuditableEntity {

    @Id
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_name"),
            inverseJoinColumns = @JoinColumn(name = "permission_name")
    )
    Set<Permission> permissions;
}

