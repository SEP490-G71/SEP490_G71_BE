package com.devintel.identityservice.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import com.devintel.identityservice.validator.DobConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationRequest {
    private String username;

    @Size(min = 8, message = "PASSWORD_INVAlID")
    private String password;

    private String firstName;
    private String lastName;

    @DobConstraint(min = 18, message = "INVALID_DOB")
    private LocalDate dob;
}
