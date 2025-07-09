package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgetPasswordRequest {

    @NotBlank(message = "FORGET_PASSWORD_USERNAME_REQUIRED")
    private String username;

    @NotBlank(message = "FORGET_PASSWORD_OLD_PASSWORD_REQUIRED")
    @Size(min = 8, message = "FORGET_PASSWORD_OLD_PASSWORD_INVALID")
    private String oldPassword;

    @NotBlank(message = "FORGET_PASSWORD_NEW_PASSWORD_REQUIRED")
    @Size(min = 8, message = "FORGET_PASSWORD_NEW_PASSWORD_INVALID")
    private String newPassword;

}
