package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkScheduleReportResponse {
    private String staffId;
    private String staffName;
    private String staffCode;
    private double attendanceRate;
    private double leaveRate;
    private int totalShifts;
    private int attendedShifts;
    private int leaveShifts;
}

