package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.common.IntegerListToStringConverter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
@Table(name = "settings")
public class Setting extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "hospital_phone")
    private String hospitalPhone;

    @Column(name = "hospital_address")
    private String hospitalAddress;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_code")
    private String bankCode;

    @Convert(converter = IntegerListToStringConverter.class)
    @Column(name = "pagination_size_list")
    private List<Integer> paginationSizeList;
}
