package vn.edu.fpt.medicaldiagnosis.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import vn.edu.fpt.medicaldiagnosis.validator.DobConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreationRequest {
    private String username;

    @Size(min = 8, message = "PASSWORD_INVAlID")
    private String password;

    private String firstName;
    private String lastName;

    @DobConstraint(min = 18, message = "INVALID_DOB")
    private LocalDate dob;
}
