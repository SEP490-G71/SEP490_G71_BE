package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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
import vn.edu.fpt.medicaldiagnosis.specification.WorkScheduleSpecification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        Set<Shift> shiftsCreated = new HashSet<>();
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
                shiftsCreated.add(Shift.MORNING);
                shiftsCreated.add(Shift.AFTERNOON);
            } else {
                schedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(request.getShift())
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());
                shiftsCreated.add(request.getShift());
            }
        }

        workScheduleRepository.saveAll(schedules);
        log.info("Created {} schedules for staff {}", schedules.size(), staff.getId());

        return WorkScheduleRecurringResponse.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .shifts(new ArrayList<>(shiftsCreated))
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

    @Override
    public Page<WorkScheduleRecurringResponse> getRecurringSchedulesPaged(
            Map<String, String> filters,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Get recurring schedules paged - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);

        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "shiftDate" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ Tách lọc thứ và ca khỏi filters (lọc ở Java)
        String filterDay = filters.remove("dayOfWeek");
        String filterShift = filters.remove("shift");

        Specification<WorkSchedule> spec = WorkScheduleSpecification.buildSpecification(filters);
        List<WorkSchedule> allSchedules = workScheduleRepository.findAll(spec, sort);

        // ✅ Nhóm theo staffId
        Map<String, List<WorkSchedule>> grouped = allSchedules.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStaff().getId()));

        DayOfWeek filterDayOfWeek = null;
        if (filterDay != null && !filterDay.isBlank()) {
            try {
                filterDayOfWeek = DayOfWeek.valueOf(filterDay.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Shift filterShiftEnum = null;
        if (filterShift != null && !filterShift.isBlank()) {
            try {
                filterShiftEnum = Shift.valueOf(filterShift.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        DayOfWeek finalFilterDayOfWeek = filterDayOfWeek;
        Shift finalFilterShift = filterShiftEnum;

        List<WorkScheduleRecurringResponse> responseList = grouped.entrySet().stream()
                .map(entry -> {
                    List<WorkSchedule> list = entry.getValue();
                    list.sort(Comparator.comparing(WorkSchedule::getShiftDate));

                    return WorkScheduleRecurringResponse.builder()
                            .staffId(entry.getKey())
                            .staffName(list.get(0).getStaff().getFullName())
                            .startDate(list.get(0).getShiftDate())
                            .endDate(list.get(list.size() - 1).getShiftDate())
                            .daysOfWeek(
                                    list.stream()
                                            .map(ws -> ws.getShiftDate().getDayOfWeek())
                                            .distinct()
                                            .sorted()
                                            .toList()
                            )
                            .shifts(
                                    list.stream()
                                            .map(WorkSchedule::getShift)
                                            .distinct()
                                            .sorted(Comparator.comparing(Enum::name))
                                            .toList()
                            )
                            .build();
                })
                .filter(res -> {
                    boolean matchDay = true;
                    boolean matchShift = true;

                    if (finalFilterDayOfWeek != null) {
                        matchDay = res.getDaysOfWeek().contains(finalFilterDayOfWeek);
                    }

                    if (finalFilterShift != null) {
                        matchShift = res.getShifts().contains(finalFilterShift);
                    }

                    return matchDay && matchShift;
                })
                .toList();

        int total = responseList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<WorkScheduleRecurringResponse> pagedList = responseList.subList(fromIndex, toIndex);

        return new PageImpl<>(pagedList, pageable, total);
    }

    @Override
    public WorkScheduleRecurringResponse getRecurringScheduleDetailByStaffId(String staffId) {
        log.info("Get recurring schedule detail for staffId: {}", staffId);

        List<WorkSchedule> schedules = workScheduleRepository
                .findAllByStaffIdAndDeletedAtIsNullOrderByShiftDateAsc(staffId);

        if (schedules.isEmpty()) {
            throw new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND);
        }

        return WorkScheduleRecurringResponse.builder()
                .staffId(staffId)
                .staffName(schedules.get(0).getStaff().getFullName())
                .startDate(schedules.get(0).getShiftDate())
                .endDate(schedules.get(schedules.size() - 1).getShiftDate())
                .daysOfWeek(
                        schedules.stream()
                                .map(ws -> ws.getShiftDate().getDayOfWeek())
                                .distinct()
                                .sorted()
                                .toList()
                )
                .shifts(
                        schedules.stream()
                                .map(WorkSchedule::getShift)
                                .distinct()
                                .sorted(Comparator.comparing(Enum::name))
                                .toList()
                )
                .build();
    }

}
