package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequest {
    @NotBlank(message = "STAFF_ID_REQUIRED")
    private String staffId;

    @NotBlank(message = "REASON_REQUIRED")
    private String reason;

    @NotEmpty(message = "DETAILS_REQUIRED")
    @Valid
    private List<LeaveRequestDetailDTO> details;
}

