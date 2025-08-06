package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredOnlineRequest {

    @NotBlank(message = "REGISTERED_FIRST_NAME_REQUIRED")
    private String firstName;

    private String middleName;

    @NotBlank(message = "REGISTERED_LAST_NAME_REQUIRED")
    private String lastName;

    @NotNull(message = "REGISTERED_DOB_REQUIRED")
    private LocalDate dob;

    @NotNull(message = "REGISTERED_GENDER_REQUIRED")
    private Gender gender;

    private String fullName;

    @NotBlank(message = "REGISTERED_EMAIL_REQUIRED")
    @Email(message = "REGISTERED_EMAIL_INVALID")
    private String email;

    @NotBlank(message = "REGISTERED_PHONE_REQUIRED")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "REGISTERED_PHONE_INVALID")
    private String phoneNumber;

    @NotNull(message = "REGISTERED_REGISTERED_AT_REQUIRED")
    private LocalDateTime registeredAt;

    private String message;
    private Status status;
    private Boolean isConfirmed;

    public String getFullName() {
        return Stream.of(firstName, middleName, lastName)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }
}
