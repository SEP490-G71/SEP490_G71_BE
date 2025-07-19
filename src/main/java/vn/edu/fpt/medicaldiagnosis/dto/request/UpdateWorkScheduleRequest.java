package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkScheduleRequest {
    @NotNull(message = "SHIFT_DATE_REQUIRED")
    private LocalDate shiftDate;

    @NotNull(message = "SHIFT_ID_REQUIRED")
    private String shiftId;

    private WorkStatus status;

    @Size(max = 255, message = "NOTE_TOO_LONG")
    private String note;
}
