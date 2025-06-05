package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @NotBlank(message = "PATIENT_FIRST_NAME_REQUIRED")
    @Size(max = 100, message = "PATIENT_FIRST_NAME_TOO_LONG")
    private String firstName;

    @NotBlank(message = "PATIENT_MIDDLE_NAME_REQUIRED")
    @Size(max = 100, message = "PATIENT_MIDDLE_NAME_TOO_LONG")
    private String middleName;

    @NotBlank(message = "PATIENT_LAST_NAME_REQUIRED")
    @Size(max = 100, message = "PATIENT_LAST_NAME_TOO_LONG")
    private String lastName;

    @NotNull(message = "PATIENT_DOB_REQUIRED")
    @Past(message = "PATIENT_DOB_PAST")
    private LocalDate dob;

    @NotNull(message = "PATIENT_GENDER_REQUIRED")
    private Gender gender;

    @NotBlank(message = "PATIENT_PHONE_REQUIRED")
    @Pattern(regexp = "^\\d{10,15}$", message = "PATIENT_PHONE_INVALID")
    private String phone;

    @NotBlank(message = "PATIENT_EMAIL_REQUIRED")
    @Email(message = "PATIENT_EMAIL_INVALID")
    private String email;

    private String accountId;
}
