package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredOnlineRequest {

    @NotBlank(message = "FULL_NAME_REQUIRED")
    private String fullName;

    @NotBlank(message = "EMAIL_REQUIRED")
    private String email;

    @NotBlank(message = "PHONE_REQUIRED")
    private String phoneNumber;

    @NotNull(message = "REGISTERED_AT_REQUIRED")
    private LocalDateTime registeredAt;

    private String message;
}
