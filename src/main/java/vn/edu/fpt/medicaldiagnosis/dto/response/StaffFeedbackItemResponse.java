package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffFeedbackItemResponse {
    private String id;

    private String staffCode;

    private String fullName;

    private String phone;

    private String email;

    private Gender gender;

    private long totalFeedbacks;

    private BigDecimal averageSatisfaction;

    private DepartmentBasicInfo department;
}
