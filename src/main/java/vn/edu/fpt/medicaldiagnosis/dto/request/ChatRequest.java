package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @NotBlank(message = "USER_ID_REQUIRED")
    private String userId;

    @NotBlank(message = "QUESTION_REQUIRED")
    private String question;
}
