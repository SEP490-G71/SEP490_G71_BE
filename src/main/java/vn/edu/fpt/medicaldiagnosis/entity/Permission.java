package vn.edu.fpt.medicaldiagnosis.entity;

// Privilege: dac quyen

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
@Table(name = "permissions")
public class Permission extends AuditableEntity{
    @Id
    private String name;

    private String description;

    @Column(name = "group_name")
    private String groupName;
}
