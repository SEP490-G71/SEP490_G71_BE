package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private String id;

    private String firstName;

    private String middleName;

    private String lastName;

    private LocalDate dob;

    private Gender gender;

    private String phone;

    private String email;

    private String accountId;

    public String getFullName() {
        return String.format("%s %s %s",
                lastName != null ? lastName.trim() : "",
                middleName != null ? middleName.trim() : "",
                firstName != null ? firstName.trim() : "").trim().replaceAll(" +", " ");
    }
}
