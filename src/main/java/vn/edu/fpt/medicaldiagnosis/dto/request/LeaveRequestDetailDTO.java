package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDetailDTO {
    @NotNull(message = "DATE_REQUIRED")
    @Future(message = "DATE_MUST_BE_TODAY_OR_FUTURE")
    private LocalDate date;

    @NotNull(message = "SHIFT_REQUIRED")
    private Shift shift;
}
