package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {
    private UUID id;

    private String firstName;

    private String middleName;

    private String lastName;

    private String staffCode;

    private String fullName;

    private String phone;

    private String email;

    private Gender gender;

    private LocalDate dob;

    private List<String> roles;

    private DepartmentBasicInfo department;
}
