package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequestByTime {
    @NotBlank(message = "STAFF_ID_REQUIRED")
    private String staffId;

    @NotBlank(message = "REASON_REQUIRED")
    private String reason;

    @NotNull(message = "FROM_DATETIME_REQUIRED")
    private LocalDateTime fromDateTime;

    @NotNull(message = "TO_DATETIME_REQUIRED")
    private LocalDateTime toDateTime;
}
