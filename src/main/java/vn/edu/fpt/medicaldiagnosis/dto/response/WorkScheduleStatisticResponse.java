package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkScheduleStatisticResponse {
    private long attendedShifts;
    private long leaveShifts;
    private long totalShifts;
    private long totalStaffs;
    private long lateShifts;
    private double attendanceRate;
    private PagedResponse<WorkScheduleReportResponse> details;
}
