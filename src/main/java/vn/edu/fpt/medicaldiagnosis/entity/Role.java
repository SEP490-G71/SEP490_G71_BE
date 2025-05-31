package vn.edu.fpt.medicaldiagnosis.entity;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Role {
    @Id
    private String name;

    private String description;

    @ManyToMany
    Set<Permission> permissions;
}
