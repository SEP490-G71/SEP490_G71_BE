package vn.edu.fpt.medicaldiagnosis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueuePatientCompactResponse {
    private String id;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private Gender gender;
    private String phone;
    private String email;

    private DepartmentType type;
    private LocalDateTime registeredTime;
    private String roomNumber;
    private String specialization;       // specialization name
    private String status;
}
