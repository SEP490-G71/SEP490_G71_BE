package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "code_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSequence {
    @Id
    @Column(name = "code_type", length = 50)
    private String codeType;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;
}
