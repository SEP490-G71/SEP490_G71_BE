package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ShiftRequest {
    @NotBlank(message = "SHIFT_NAME_REQUIRED")
    private String name;

    @NotNull(message = "SHIFT_START_TIME_REQUIRED")
    private LocalTime startTime;

    @NotNull(message = "SHIFT_END_TIME_REQUIRED")
    private LocalTime endTime;

    private String description;
}

