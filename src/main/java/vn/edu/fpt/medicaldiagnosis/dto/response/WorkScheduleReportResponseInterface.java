package vn.edu.fpt.medicaldiagnosis.dto.response;

public interface WorkScheduleReportResponseInterface {
    String getStaffId();
    String getStaffName();
    String getStaffCode();
    Integer getTotalShifts();
    Integer getAttendedShifts();
    Integer getLeaveShifts();
    Double getAttendanceRate();
    Double getLeaveRate();
}
