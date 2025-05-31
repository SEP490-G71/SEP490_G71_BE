package vn.edu.fpt.medicaldiagnosis.entity;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InvalidatedToken {
    @Id
    private String id;

    private Date expireTime;
}
