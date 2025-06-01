package vn.edu.fpt.medicaldiagnosis.entity;

import java.util.Date;

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
@Table(name = "invalidated_tokens")
public class InvalidatedToken {
    @Id
    private String id;

    private Date expireTime;
}
