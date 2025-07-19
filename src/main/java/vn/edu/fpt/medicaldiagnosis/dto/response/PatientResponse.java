package vn.edu.fpt.medicaldiagnosis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponse {

    private String id;

    private String patientCode;

    private String firstName;

    private String middleName;

    private String lastName;

    private String fullName;

    private LocalDate dob;

    private Gender gender;

    private String phone;

    private String email;

    private DepartmentType type;

    private LocalDateTime registeredTime;

    private String roomNumber;

    private String specialization;
}
