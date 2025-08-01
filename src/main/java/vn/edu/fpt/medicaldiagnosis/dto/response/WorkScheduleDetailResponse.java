package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import vn.edu.fpt.medicaldiagnosis.entity.Shift;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkScheduleDetailResponse {
    private String id;
    private String staffId;
    private String staffName;
    private LocalDate shiftDate;
    private ShiftResponse shift;
    private WorkStatus status;
    private String note;
}

