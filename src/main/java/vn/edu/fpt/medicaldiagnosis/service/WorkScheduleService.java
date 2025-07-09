package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleCreateResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleRecurringResponse;


import java.util.List;

public interface WorkScheduleService {
    WorkScheduleRecurringResponse createRecurringSchedules(WorkScheduleRecurringRequest request);

    WorkScheduleCreateResponse checkIn(String scheduleId);

    List<WorkScheduleDetailResponse> getAllSchedulesByStaffId(String staffId);

}
