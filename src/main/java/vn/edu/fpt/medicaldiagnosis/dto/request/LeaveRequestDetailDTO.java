package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDetailDTO {
    @NotNull(message = "DATE_REQUIRED")
    @Future(message = "DATE_MUST_BE_TODAY_OR_FUTURE")
    private LocalDate date;

    @NotBlank(message = "SHIFT_ID_REQUIRED")
    private String shiftId;
}
