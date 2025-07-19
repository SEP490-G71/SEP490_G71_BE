package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkScheduleRecurringRequest {
    @NotBlank(message = "STAFF_ID_REQUIRED")
    private String staffId;

    @NotEmpty(message = "SHIFTS_REQUIRED")
    private List<String> shiftIds;

    @NotEmpty(message = "DAYS_OF_WEEK_REQUIRED")
    private List<DayOfWeek> daysOfWeek;

    @NotNull(message = "START_DATE_REQUIRED")
    @FutureOrPresent(message = "START_DATE_MUST_BE_NOW_OR_FUTURE")
    private LocalDate startDate;

    @NotNull(message = "END_DATE_REQUIRED")
    @Future(message = "END_DATE_MUST_BE_IN_FUTURE")
    private LocalDate endDate;

    @Size(max = 255, message = "NOTE_TOO_LONG")
    private String note;
}
