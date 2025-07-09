package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkScheduleRecurringResponse {
    private String staffId;
    private String staffName;
    private Shift shift;
    private List<DayOfWeek> daysOfWeek;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
}

