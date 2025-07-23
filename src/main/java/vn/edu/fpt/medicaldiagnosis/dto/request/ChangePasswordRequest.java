package vn.edu.fpt.medicaldiagnosis.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "OLD_PASSWORD_EMPTY")
    private String oldPassword;

    @NotBlank(message = "NEW_PASSWORD_EMPTY")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "NEW_PASSWORD_INVALID"
    )
    private String newPassword;

    @NotBlank(message = "CONFIRM_NEW_PASSWORD_EMPTY")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "CONFIRM_NEW_PASSWORD_INVALID"
    )
    private String confirmNewPassword;
}