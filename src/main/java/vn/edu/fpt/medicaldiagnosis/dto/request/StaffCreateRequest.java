package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
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
public class StaffCreateRequest {

    @NotBlank(message = "STAFF_FIRST_NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "STAFF_FIRST_NAME_LENGTH")
    private String firstName;

    @Size(max = 100, message = "STAFF_MIDDLE_NAME_LENGTH")
    private String middleName;

    @NotBlank(message = "STAFF_LAST_NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "STAFF_LAST_NAME_LENGTH")
    private String lastName;

    @NotBlank(message = "STAFF_PHONE_EMPTY")
    @Pattern(regexp = "\\d{10}", message = "STAFF_PHONE_INVALID")
    private String phone;

    @Email(message = "STAFF_EMAIL_INVALID")
    @NotBlank(message = "STAFF_EMAIL_EMPTY")
    @Size(min = 3, max = 100, message = "STAFF_EMAIL_LENGTH")
    private String email;

    @NotNull(message = "STAFF_GENDER_EMPTY")
    private Gender gender;

    @Past(message = "STAFF_DOB_PAST")
    @NotNull(message = "STAFF_DOB_EMPTY")
    private LocalDate dob;

    private String accountId;

    @NotEmpty(message = "STAFF_ROLE_NAMES_EMPTY")
    private List<String> roleNames;
}
