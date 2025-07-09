package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleCreateResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleRecurringResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.WorkScheduleMapper;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.WorkScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Transactional
public class WorkScheduleServiceImpl implements WorkScheduleService {
    StaffRepository staffRepository;
    WorkScheduleRepository workScheduleRepository;
    WorkScheduleMapper workScheduleMapper;
    @Override
    public WorkScheduleRecurringResponse createRecurringSchedules(WorkScheduleRecurringRequest request) {
        log.info("Service: Create recurring schedules - {}", request);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        List<WorkSchedule> schedules = new ArrayList<>();

        for (LocalDate date = request.getStartDate();
             !date.isAfter(request.getEndDate());
             date = date.plusDays(1)) {

            if (!request.getDaysOfWeek().contains(date.getDayOfWeek())) continue;

            if (request.getShift() == Shift.FULL_DAY) {
                schedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(Shift.MORNING)
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());

                schedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(Shift.AFTERNOON)
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());
            } else {
                schedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(request.getShift())
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());
            }
        }

        workScheduleRepository.saveAll(schedules);
        log.info("Created {} schedules for staff {}", schedules.size(), staff.getId());

        return WorkScheduleRecurringResponse.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .shift(request.getShift())
                .daysOfWeek(request.getDaysOfWeek())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .note(request.getNote())
                .build();
    }


    @Override
    public WorkScheduleCreateResponse checkIn(String scheduleId) {
        log.info("Check-in for workSchedule id: {}", scheduleId);

        WorkSchedule schedule = workScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND));

//        String currentStaffId = SecurityUtil.getCurrentStaffId();
//        if (!schedule.getStaff().getId().equals(currentStaffId)) {
//            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
//        }
        // ✅ Check chỉ cho điểm danh đúng ngày hôm nay
//        LocalDate today = LocalDate.now();
//        if (!schedule.getShiftDate().isEqual(today)) {
//            throw new AppException(ErrorCode.CHECKIN_DATE_INVALID);
//        }

        if (schedule.getCheckInTime() != null) {
            throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_CHECKED_IN);
        }

        schedule.setCheckInTime(LocalDateTime.now());
        schedule.setStatus(WorkStatus.ATTEND);
        workScheduleRepository.save(schedule);

        return workScheduleMapper.toCreateResponse(schedule);
    }

    @Override
    public List<WorkScheduleDetailResponse> getAllSchedulesByStaffId(String staffId) {
        log.info("Get all schedules for staff: {}", staffId);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        log.info("Get all schedules for staff: {} - {}", staffId, staff.getFullName());

        List<WorkSchedule> schedules = workScheduleRepository.findAllByStaffIdAndDeletedAtIsNullOrderByShiftDateAsc(staffId);

        return schedules.stream()
                .map(workScheduleMapper::toDetailResponse)
                .toList();
    }

}
