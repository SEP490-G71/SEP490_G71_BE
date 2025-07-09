package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleCreateResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleRecurringResponse;


import java.util.List;
import java.util.Map;

public interface WorkScheduleService {
    WorkScheduleRecurringResponse createRecurringSchedules(WorkScheduleRecurringRequest request);

    WorkScheduleCreateResponse checkIn(String scheduleId);

    List<WorkScheduleDetailResponse> getAllSchedulesByStaffId(String staffId);

    Page<WorkScheduleRecurringResponse> getRecurringSchedulesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    WorkScheduleRecurringResponse getRecurringScheduleDetailByStaffId(String staffId);

    WorkScheduleRecurringResponse updateRecurringSchedules(WorkScheduleRecurringRequest request);

    WorkScheduleDetailResponse updateWorkSchedule(String id, UpdateWorkScheduleRequest request);
}
