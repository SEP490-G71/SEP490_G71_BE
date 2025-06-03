package vn.edu.fpt.medicaldiagnosis.entity;

// Privilege: dac quyen

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    private String name;

    private String description;

    @Column(name = "group_name")
    private String groupName;
}
