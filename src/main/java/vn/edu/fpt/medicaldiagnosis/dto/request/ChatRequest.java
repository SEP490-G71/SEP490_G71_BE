package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "USER_ID_REQUIRED")
    private String userId;

    @NotBlank(message = "QUESTION_REQUIRED")
    private String question;
}
