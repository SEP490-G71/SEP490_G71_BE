package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.WorkScheduleMapper;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.WorkScheduleService;
import vn.edu.fpt.medicaldiagnosis.specification.WorkScheduleSpecification;
import vn.edu.fpt.medicaldiagnosis.specification.WorkScheduleStatisticSpecification;

import java.nio.charset.StandardCharsets;
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
    EmailTaskRepository emailTaskRepository;
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

        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            sendWorkScheduleChangedEmail(staff);
        }

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
        schedule.setStatus(WorkStatus.ATTENDED);
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

    @Override
    public WorkScheduleRecurringResponse updateRecurringSchedules(WorkScheduleRecurringRequest request) {
        log.info("Service: Update recurring schedules - {}", request);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Xoá tất cả lịch cũ trong khoảng thời gian đã chọn
        List<WorkSchedule> existing = workScheduleRepository.findAllByStaffIdAndShiftDateBetweenAndDeletedAtIsNull(
                request.getStaffId(), request.getStartDate(), request.getEndDate()
        );

        workScheduleRepository.deleteAll(existing);

        // Tạo lịch mới
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

        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            sendWorkScheduleChangedEmail(staff);
        }

        return WorkScheduleRecurringResponse.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysOfWeek(request.getDaysOfWeek())
                .shifts(
                        request.getShift() == Shift.FULL_DAY ?
                                List.of(Shift.MORNING, Shift.AFTERNOON) :
                                List.of(request.getShift())
                )
                .note(request.getNote())
                .build();
    }

    @Override
    public WorkScheduleDetailResponse updateWorkSchedule(String id, UpdateWorkScheduleRequest request) {
        WorkSchedule schedule = workScheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND));

        // Không cho cập nhật nếu lịch đã được chấm công
        if (WorkStatus.ATTENDED.equals(schedule.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_ATTENDED_SCHEDULE);
        }

        // Check nếu lịch đã qua thì không cho chỉnh
        if (schedule.getShiftDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_PAST_SCHEDULE);
        }

        // Check nếu muốn sửa sang ngày khác trong quá khứ thì cũng không cho
        if (request.getShiftDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_MOVE_SCHEDULE_TO_PAST);
        }

        // Cập nhật thông tin
        schedule.setShiftDate(request.getShiftDate());
        schedule.setShift(request.getShift());
        schedule.setStatus(request.getStatus() != null ? request.getStatus() : WorkStatus.SCHEDULED);
        schedule.setNote(request.getNote());

        workScheduleRepository.save(schedule);

        // Gửi email thông báo cho nhân viên
        Staff staff = schedule.getStaff();
        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            sendWorkScheduleChangedEmail(staff);
        }

        return WorkScheduleDetailResponse.builder()
                .id(schedule.getId())
                .staffId(schedule.getStaff().getId())
                .staffName(schedule.getStaff().getFullName())
                .shift(schedule.getShift())
                .shiftDate(schedule.getShiftDate())
                .status(schedule.getStatus())
                .note(schedule.getNote())
                .build();
    }

    @Override
    public void deleteWorkSchedule(String id) {
        log.info("Start hard deleting work schedule: {}", id);

        WorkSchedule schedule = workScheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND));

        if (WorkStatus.ATTENDED.equals(schedule.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_ATTENDED_SCHEDULE);
        }

        if (schedule.getShiftDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_PAST_SCHEDULE);
        }

        workScheduleRepository.deleteByIdHard(id);
    }

    @Override
    public void deleteWorkSchedulesByStaffId(String staffId) {
        log.info("Start hard deleting work schedules for staff: {}", staffId);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            sendWorkScheduleChangedEmail(staff);
        }

        workScheduleRepository.deleteFutureUnattendedByStaffId(staffId, LocalDate.now());
        log.info("Đã xóa toàn bộ lịch chưa làm và chưa chấm công trong tương lai cho nhân viên {}", staffId);
    }

    @Override
    public WorkScheduleStatisticResponse getWorkScheduleStatistics(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Get work schedule statistics");
        Specification<WorkSchedule> spec = WorkScheduleStatisticSpecification.buildSpecification(filters);
        List<WorkSchedule> schedules = workScheduleRepository.findAll(spec);

        // Group by staff
        Map<String, WorkScheduleReportResponse> grouped = new HashMap<>();
        for (WorkSchedule schedule : schedules) {
            Staff staff = schedule.getStaff();
            String staffId = staff.getId();

            WorkScheduleReportResponse report = grouped.computeIfAbsent(staffId, id -> WorkScheduleReportResponse.builder()
                    .staffId(staff.getId())
                    .staffCode(staff.getStaffCode())
                    .staffName(staff.getFullName())
                    .totalShifts(0)
                    .attendedShifts(0)
                    .leaveShifts(0)
                    .build());

            report.setTotalShifts(report.getTotalShifts() + 1);

            switch (schedule.getStatus()) {
                case ATTENDED -> report.setAttendedShifts(report.getAttendedShifts() + 1);
                case ON_LEAVE -> report.setLeaveShifts(report.getLeaveShifts() + 1);
            }
        }

        // Set rates
        List<WorkScheduleReportResponse> results = new ArrayList<>(grouped.values());
        results.forEach(r -> {
            r.setAttendanceRate(r.getTotalShifts() == 0 ? 0 : (float) r.getAttendedShifts() / r.getTotalShifts() * 100);
            r.setLeaveRate(r.getTotalShifts() == 0 ? 0 : (float) r.getLeaveShifts() / r.getTotalShifts() * 100);
        });

        // Sort
        Comparator<WorkScheduleReportResponse> comparator = switch (sortBy) {
            case "staffName" -> Comparator.comparing(WorkScheduleReportResponse::getStaffName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "attendanceRate" -> Comparator.comparing(WorkScheduleReportResponse::getAttendanceRate);
            default -> Comparator.comparing(WorkScheduleReportResponse::getStaffCode);
        };

        if (sortDir.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        }
        results.sort(comparator);

        // Paging
        int totalElements = results.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        List<WorkScheduleReportResponse> pageContent = results.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        long totalShifts = results.stream().mapToInt(WorkScheduleReportResponse::getTotalShifts).sum();
        long attendedShifts = results.stream().mapToInt(WorkScheduleReportResponse::getAttendedShifts).sum();
        long leaveShifts = results.stream().mapToInt(WorkScheduleReportResponse::getLeaveShifts).sum();
        long totalStaffs = grouped.size();
        double attendanceRate = totalShifts == 0 ? 0 : (double) attendedShifts / totalShifts * 100;

        return WorkScheduleStatisticResponse.builder()
                .totalShifts(totalShifts)
                .attendedShifts(attendedShifts)
                .leaveShifts(leaveShifts)
                .totalStaffs(totalStaffs)
                .attendanceRate(attendanceRate)
                .details(new PagedResponse<>(pageContent, page, size, totalElements, totalPages, (page + 1) * size >= totalElements))
                .build();
    }

    private void sendWorkScheduleChangedEmail(Staff staff) {
        String tenantId = TenantContext.getTenantId();
        String content;

        try {
            ClassPathResource resource = new ClassPathResource("templates/work-schedule-update-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template.replace("{{staffName}}", staff.getFullName());
        } catch (Exception e) {
            content = String.format("""
                Xin chào %s,

                Lịch làm việc của bạn đã được thay đổi. Vui lòng đăng nhập vào hệ thống để kiểm tra và cập nhật thông tin.

                Trân trọng,
                Phòng Quản lý lịch làm việc.
                """, staff.getFullName());
            log.warn("[{}] Không thể load template work-schedule-update-email.html: {}", tenantId, e.getMessage());
        }

        EmailTask task = EmailTask.builder()
                .id(UUID.randomUUID().toString())
                .emailTo(staff.getEmail())
                .subject("Thông báo lịch làm việc")
                .content(content)
                .retryCount(0)
                .status(Status.PENDING)
                .build();

        emailTaskRepository.save(task);
        log.info("[{}] Đã tạo email thông báo thay đổi lịch cho staff {}", tenantId, staff.getId());
    }
}
