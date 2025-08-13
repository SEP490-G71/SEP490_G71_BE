package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.BulkUpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateWorkScheduleRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.WorkScheduleRecurringRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.ShiftMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.WorkScheduleMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;
import vn.edu.fpt.medicaldiagnosis.service.WorkScheduleService;
import vn.edu.fpt.medicaldiagnosis.specification.WorkScheduleSpecification;
import vn.edu.fpt.medicaldiagnosis.specification.WorkScheduleStatisticSpecification;

import java.nio.charset.StandardCharsets;
import java.time.*;
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
    ShiftRepository shiftRepository;
    ShiftMapper shiftMapper;
    AccountRepository accountRepository;
    SettingService settingService;
    @Override
    public WorkScheduleRecurringResponse createRecurringSchedules(WorkScheduleRecurringRequest request) {
        log.info("Service: Create recurring schedules - {}", request);

        Set<String> uniqueShiftIds = new HashSet<>(request.getShiftIds());
        if (uniqueShiftIds.size() < request.getShiftIds().size()) {
            throw new AppException(ErrorCode.DUPLICATE_SHIFT_IDS);
        }

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        List<Shift> shifts = shiftRepository.findAllById(request.getShiftIds());
        if (shifts.size() != request.getShiftIds().size()) {
            throw new AppException(ErrorCode.SHIFT_NOT_FOUND);
        }

        // ✅ Chuẩn bị tất cả ngày cần check
        List<LocalDate> allDates = new ArrayList<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            if (request.getDaysOfWeek().contains(date.getDayOfWeek())) {
                allDates.add(date);
            }
        }

        // ✅ Lấy lịch đã tồn tại từ DB trong 1 query
        List<WorkSchedule> existing = workScheduleRepository
                .findAllByStaffIdAndShiftDateInAndShiftIdIn(staff.getId(), allDates, request.getShiftIds());

        // ✅ Tạo Map để tra cứu nhanh
        Set<String> existingKeys = existing.stream()
                .map(ws -> ws.getShiftDate() + "_" + ws.getShift().getId())
                .collect(Collectors.toSet());

        // ✅ Tạo danh sách mới
        List<WorkSchedule> schedules = new ArrayList<>();
        for (LocalDate date : allDates) {
            for (Shift shift : shifts) {
                String key = date + "_" + shift.getId();
                if (existingKeys.contains(key)) {
                    throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS,
                            String.format("Đã tồn tại lịch làm cho nhân viên [%s] ngày [%s] ca [%s]",
                                    staff.getFullName(), date, shift.getName()));
                }

                schedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(shift)
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());
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
                .shifts(shiftMapper.toResponseList(shifts))
                .daysOfWeek(request.getDaysOfWeek())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .note(request.getNote())
                .build();
    }



    @Override
    public WorkScheduleCreateResponse checkIn(String scheduleId) {
        WorkSchedule schedule = workScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND));

        if (schedule.getStatus() == WorkStatus.ON_LEAVE) {
            throw new AppException(ErrorCode.CHECKIN_NOT_ALLOWED, "Ca này đang xin nghỉ phép, không thể check-in.");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));
        Staff currentStaff = staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));

        if (!schedule.getStaff().getId().equals(currentStaff.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        LocalDate today = LocalDate.now();
        if (!schedule.getShiftDate().isEqual(today)) {
            throw new AppException(ErrorCode.CHECKIN_DATE_INVALID);
        }

        if (schedule.getCheckInTime() != null) {
            throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_CHECKED_IN);
        }

        final int LATE_LIMIT = Optional.ofNullable(settingService.getSetting().getLatestCheckInMinutes()).orElse(15);

        LocalDate shiftDate = schedule.getShiftDate();
        LocalTime start = schedule.getShift().getStartTime();
        LocalTime end   = schedule.getShift().getEndTime();
        boolean overnight = end.isBefore(start);

        // now theo TZ hệ thống/tenant (nếu có ZoneId riêng thì dùng ZonedDateTime.now(zoneId))
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startDT = LocalDateTime.of(shiftDate, start);
        LocalDateTime endDT   = LocalDateTime.of(shiftDate, end);
        if (overnight) endDT = endDT.plusDays(1); // ca qua đêm kết thúc ngày hôm sau
        log.info("startDT: {}, endDT: {}", startDT, endDT);
        log.info("now: {}", now);

        LocalDateTime earliest = startDT.minusMinutes(15);

        // CHỈ cho check-in trong [startDT, endDT]
        if (now.isBefore(earliest)) {
            throw new AppException(ErrorCode.CHECKIN_TIME_INVALID,
                    "Bạn đang check-in sớm. Chỉ được check-in trước giờ bắt đầu ca tối đa 15 phút.");
        }
        if (now.isAfter(endDT)) {
            throw new AppException(ErrorCode.CHECKIN_TIME_INVALID, "Ca đã kết thúc, không thể check-in.");
        }

        long minutesLate = Math.max(0, Duration.between(startDT, now).toMinutes());

        schedule.setCheckInTime(now);
        schedule.setStatus(minutesLate > LATE_LIMIT ? WorkStatus.LATE : WorkStatus.ATTENDED);
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
        String filterShiftId = filters.remove("shiftId");

        Specification<WorkSchedule> spec = WorkScheduleSpecification.buildSpecification(filters)
                .and((root, query, cb) -> cb.isNull(root.get("staff").get("deletedAt")));
        List<WorkSchedule> allSchedules = workScheduleRepository.findAll(spec, sort);

        // ✅ Lọc theo thứ
        DayOfWeek filterDayOfWeek = null;
        if (filterDay != null && !filterDay.isBlank()) {
            try {
                filterDayOfWeek = DayOfWeek.valueOf(filterDay.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // ✅ Lọc shift từ DB theo name (nếu có)
        Shift filterShiftEntity = null;
        if (filterShiftId != null && !filterShiftId.isBlank()) {
            filterShiftEntity = shiftRepository.findById(filterShiftId).orElse(null);
        }

        DayOfWeek finalFilterDayOfWeek = filterDayOfWeek;
        Shift finalFilterShiftEntity = filterShiftEntity;

        // ✅ Nhóm theo staffId
        Map<String, List<WorkSchedule>> grouped = allSchedules.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStaff().getId()));

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
                                    shiftMapper.toResponseList(
                                            list.stream()
                                                    .map(WorkSchedule::getShift)
                                                    .filter(Objects::nonNull)
                                                    .distinct()
                                                    .sorted(Comparator.comparing(Shift::getName))
                                                    .collect(Collectors.toList())
                                    )
                            )
                            .note(null)
                            .build();
                })
                .filter(res -> {
                    boolean matchDay = true;
                    boolean matchShift = true;

                    if (finalFilterDayOfWeek != null) {
                        matchDay = res.getDaysOfWeek().contains(finalFilterDayOfWeek);
                    }

                    if (finalFilterShiftEntity != null) {
                        matchShift = allSchedules.stream()
                                .filter(ws -> ws.getStaff().getId().equals(res.getStaffId()))
                                .anyMatch(ws -> ws.getShift().getId().equals(finalFilterShiftEntity.getId()));
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
        log.info("Service: get recurring schedule detail for staffId: {}", staffId);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        log.info("Get recurring schedule detail for staffId: {} - {}", staffId, staff.getFullName());
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
                        shiftMapper.toResponseList( // 🔄 Map List<Shift> → List<ShiftResponse>
                                schedules.stream()
                                        .map(WorkSchedule::getShift)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .sorted(Comparator.comparing(Shift::getName))
                                        .toList()
                        )
                )
                .build();
    }

    @Override
    public WorkScheduleRecurringResponse updateRecurringSchedules(WorkScheduleRecurringRequest request) {
        log.info("Service: Update recurring schedules - {}", request);

        // ✅ Check trùng shiftId
        Set<String> uniqueShiftIds = new HashSet<>(request.getShiftIds());
        if (uniqueShiftIds.size() < request.getShiftIds().size()) {
            throw new AppException(ErrorCode.DUPLICATE_SHIFT_IDS);
        }

        // ✅ Tìm nhân viên
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // ✅ Tìm danh sách ca làm việc
        List<Shift> shifts = shiftRepository.findAllById(request.getShiftIds());
        if (shifts.size() != request.getShiftIds().size()) {
            throw new AppException(ErrorCode.SHIFT_NOT_FOUND);
        }

        // ✅ Chuẩn bị danh sách ngày hợp lệ theo yêu cầu
        List<LocalDate> allDates = new ArrayList<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            if (request.getDaysOfWeek().contains(date.getDayOfWeek())) {
                allDates.add(date);
            }
        }

        // ✅ Xoá toàn bộ lịch làm việc của nhân viên trong khoảng thời gian (không phân biệt ca nào)
        List<WorkSchedule> existing = workScheduleRepository
                .findAllByStaffIdAndShiftDateBetweenAndDeletedAtIsNull(
                        staff.getId(), request.getStartDate(), request.getEndDate());

        workScheduleRepository.deleteAll(existing);
        log.info("Deleted {} old schedules", existing.size());

        // ✅ Tạo danh sách mới
        List<WorkSchedule> newSchedules = new ArrayList<>();
        for (LocalDate date : allDates) {
            for (Shift shift : shifts) {
                newSchedules.add(WorkSchedule.builder()
                        .staff(staff)
                        .shiftDate(date)
                        .shift(shift)
                        .status(WorkStatus.SCHEDULED)
                        .note(request.getNote())
                        .build());
            }
        }

        workScheduleRepository.saveAll(newSchedules);
        log.info("Created {} new schedules", newSchedules.size());

        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            sendWorkScheduleChangedEmail(staff);
        }

        return WorkScheduleRecurringResponse.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .shifts(shiftMapper.toResponseList(shifts))
                .daysOfWeek(request.getDaysOfWeek())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
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

        // Không cho cập nhật nếu lịch đã qua
        if (schedule.getShiftDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_PAST_SCHEDULE);
        }

        // Không cho cập nhật sang ngày trong quá khứ
        if (request.getShiftDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_MOVE_SCHEDULE_TO_PAST);
        }

        // ✅ Tìm shift entity theo ID
        Shift newShift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new AppException(ErrorCode.SHIFT_NOT_FOUND));

        // ✅ Check nếu lịch đã tồn tại với shift mới (trừ bản ghi hiện tại)
        boolean isDuplicate = workScheduleRepository.existsByStaffIdAndShiftDateAndShiftAndIdNot(
                schedule.getStaff().getId(),
                request.getShiftDate(),
                newShift,
                schedule.getId()
        );
        if (isDuplicate) {
            throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS);
        }

        // ✅ Cập nhật thông tin
        schedule.setShiftDate(request.getShiftDate());
        schedule.setShift(newShift);
        schedule.setStatus(request.getStatus() != null ? request.getStatus() : WorkStatus.SCHEDULED);
        schedule.setNote(request.getNote());
        log.info("Updated work schedule: {}", schedule);
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
                .shift(shiftMapper.toResponse(schedule.getShift()))
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
        Specification<WorkSchedule> spec = WorkScheduleStatisticSpecification.buildSpecification(filters)
                .and((root, query, cb) -> cb.isNull(root.get("staff").get("deletedAt")));
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
                    .lateRate(0)
                    .build());

            report.setTotalShifts(report.getTotalShifts() + 1);

            switch (schedule.getStatus()) {
                case ATTENDED -> report.setAttendedShifts(report.getAttendedShifts() + 1);
                case ON_LEAVE -> report.setLeaveShifts(report.getLeaveShifts() + 1);
                case LATE -> report.setLateShifts(report.getLateShifts() + 1);
            }
        }

        // Set rates
        List<WorkScheduleReportResponse> results = new ArrayList<>(grouped.values());
        results.forEach(r -> {
            r.setAttendanceRate(r.getTotalShifts() == 0 ? 0 : (float) r.getAttendedShifts() / r.getTotalShifts() * 100);
            r.setLeaveRate(r.getTotalShifts() == 0 ? 0 : (float) r.getLeaveShifts() / r.getTotalShifts() * 100);
            r.setLateRate(r.getTotalShifts() == 0 ? 0 : (float) r.getLateShifts() / r.getTotalShifts() * 100);
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
        long lateShifts     = results.stream().mapToInt(WorkScheduleReportResponse::getLateShifts).sum();
        long totalStaffs = grouped.size();
        double attendanceRate = totalShifts == 0 ? 0 : (double) attendedShifts / totalShifts * 100;

        return WorkScheduleStatisticResponse.builder()
                .totalShifts(totalShifts)
                .attendedShifts(attendedShifts)
                .leaveShifts(leaveShifts)
                .lateShifts(lateShifts)
                .totalStaffs(totalStaffs)
                .attendanceRate(attendanceRate)
                .details(new PagedResponse<>(pageContent, page, size, totalElements, totalPages, (page + 1) * size >= totalElements))
                .build();
    }

//    @Override
//    public List<WorkScheduleDetailResponse> bulkUpdateWorkSchedules(String staffId, BulkUpdateWorkScheduleRequest request) {
//        log.info("Bulk update work schedules - {}", request);
//
//        // Tìm nhân viên
//        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
//                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
//
//        boolean hasChanged = false;
//        List<WorkSchedule> toCreate = new ArrayList<>();
//
//        // ======= 1. XỬ LÝ XOÁ =======
//        if (request.getIdsToDelete() != null && !request.getIdsToDelete().isEmpty()) {
//            List<WorkSchedule> toDelete = workScheduleRepository.findAllById(request.getIdsToDelete());
//
//            for (WorkSchedule schedule : toDelete) {
//                if (schedule.getDeletedAt() != null) continue;
//
//                if (WorkStatus.ATTENDED.equals(schedule.getStatus())) {
//                    throw new AppException(ErrorCode.CANNOT_DELETE_ATTENDED_SCHEDULE,
//                            String.format("Không thể xoá lịch [%s] đã chấm công", schedule.getId()));
//                }
//
//                if (schedule.getShiftDate().isBefore(LocalDate.now())) {
//                    throw new AppException(ErrorCode.CANNOT_DELETE_PAST_SCHEDULE,
//                            String.format("Không thể xoá lịch [%s] đã qua ngày", schedule.getId()));
//                }
//            }
//
//            workScheduleRepository.deleteAll(toDelete);
//            log.info("Deleted {} schedules", toDelete.size());
//            hasChanged = true;
//        }
//
//        // ======= 2. XỬ LÝ TẠO MỚI =======
//        if (request.getNewSchedules() != null && !request.getNewSchedules().isEmpty()) {
//            List<UpdateWorkScheduleRequest> newSchedules = request.getNewSchedules();
//
//            // Kiểm tra trùng lặp trong request
//            Set<String> inputKeys = new HashSet<>();
//            for (UpdateWorkScheduleRequest item : newSchedules) {
//                String key = item.getShiftDate() + "_" + item.getShiftId();
//                if (!inputKeys.add(key)) {
//                    throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS,
//                            String.format("Trùng lịch trong yêu cầu tạo mới: ngày [%s] - ca [%s]", item.getShiftDate(), item.getShiftId()));
//                }
//            }
//
//            // Truy vấn lịch đã tồn tại và shift
//            List<LocalDate> shiftDates = newSchedules.stream().map(UpdateWorkScheduleRequest::getShiftDate).distinct().toList();
//            List<String> shiftIds = newSchedules.stream().map(UpdateWorkScheduleRequest::getShiftId).distinct().toList();
//
//            List<WorkSchedule> existing = workScheduleRepository
//                    .findAllByStaffIdAndShiftDateInAndShiftIdIn(staffId, shiftDates, shiftIds);
//            Set<String> existingKeys = existing.stream()
//                    .map(ws -> ws.getShiftDate() + "_" + ws.getShift().getId())
//                    .collect(Collectors.toSet());
//
//            Map<String, Shift> shiftMap = shiftRepository.findAllById(shiftIds).stream()
//                    .collect(Collectors.toMap(Shift::getId, s -> s));
//
//            for (UpdateWorkScheduleRequest item : newSchedules) {
//                String key = item.getShiftDate() + "_" + item.getShiftId();
//                if (existingKeys.contains(key)) {
//                    throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS,
//                            String.format("Đã tồn tại lịch làm việc: ngày [%s] - ca [%s]", item.getShiftDate(), item.getShiftId()));
//                }
//
//                Shift shift = shiftMap.get(item.getShiftId());
//                if (shift == null) {
//                    throw new AppException(ErrorCode.SHIFT_NOT_FOUND,
//                            String.format("Không tìm thấy ca làm với ID: %s", item.getShiftId()));
//                }
//
//                toCreate.add(WorkSchedule.builder()
//                        .shiftDate(item.getShiftDate())
//                        .shift(shift)
//                        .staff(staff)
//                        .status(item.getStatus() != null ? item.getStatus() : WorkStatus.SCHEDULED)
//                        .note(item.getNote())
//                        .build());
//            }
//
//            workScheduleRepository.saveAll(toCreate);
//            log.info("Created {} new schedules", toCreate.size());
//            hasChanged = true;
//        }
//
//        // ======= 3. GỬI MAIL NẾU CÓ THAY ĐỔI =======
//        if (hasChanged) {
//            sendWorkScheduleChangedEmail(staff);
//        }
//
//        // ======= 4. TRẢ VỀ RESPONSE =======
//        return toCreate.stream()
//                .map(s -> WorkScheduleDetailResponse.builder()
//                        .id(s.getId())
//                        .shiftDate(s.getShiftDate())
//                        .shift(shiftMapper.toResponse(s.getShift()))
//                        .staffId(staffId)
//                        .staffName(staff.getFullName())
//                        .status(s.getStatus())
//                        .note(s.getNote())
//                        .build())
//                .toList();
//    }

    @Override
    public List<WorkScheduleDetailResponse> bulkUpdateWorkSchedules(String staffId, BulkUpdateWorkScheduleRequest request) {
        log.info("Bulk update work schedules - {}", request);

        // ====== 0. Tìm nhân viên ======
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // ====== 0.1 Validate trước khi xử lý ======
        validateNewSchedules(staffId, request.getNewSchedules());

        boolean hasChanged = false;
        List<WorkSchedule> toCreate = new ArrayList<>();

        // ======= 1. XỬ LÝ XOÁ =======
        if (request.getIdsToDelete() != null && !request.getIdsToDelete().isEmpty()) {
            List<WorkSchedule> toDelete = workScheduleRepository.findAllById(request.getIdsToDelete());

            for (WorkSchedule schedule : toDelete) {
                if (schedule.getDeletedAt() != null) continue;

                if (WorkStatus.ATTENDED.equals(schedule.getStatus())) {
                    throw new AppException(ErrorCode.CANNOT_DELETE_ATTENDED_SCHEDULE,
                            String.format("Không thể xoá lịch [%s] đã chấm công", schedule.getId()));
                }

                if (schedule.getShiftDate().isBefore(LocalDate.now())) {
                    throw new AppException(ErrorCode.CANNOT_DELETE_PAST_SCHEDULE,
                            String.format("Không thể xoá lịch [%s] đã qua ngày", schedule.getId()));
                }
            }

            workScheduleRepository.deleteAll(toDelete);
            log.info("Deleted {} schedules", toDelete.size());
            hasChanged = true;
        }

        // ======= 2. XỬ LÝ TẠO MỚI =======
        if (request.getNewSchedules() != null && !request.getNewSchedules().isEmpty()) {
            List<UpdateWorkScheduleRequest> newSchedules = request.getNewSchedules();

            List<String> shiftIds = newSchedules.stream()
                    .map(UpdateWorkScheduleRequest::getShiftId)
                    .distinct()
                    .toList();

            Map<String, Shift> shiftMap = shiftRepository.findAllById(shiftIds).stream()
                    .collect(Collectors.toMap(Shift::getId, s -> s));

            for (UpdateWorkScheduleRequest item : newSchedules) {
                Shift shift = shiftMap.get(item.getShiftId());
                toCreate.add(WorkSchedule.builder()
                        .shiftDate(item.getShiftDate())
                        .shift(shift)
                        .staff(staff)
                        .status(item.getStatus() != null ? item.getStatus() : WorkStatus.SCHEDULED)
                        .note(item.getNote())
                        .build());
            }

            workScheduleRepository.saveAll(toCreate);
            log.info("Created {} new schedules", toCreate.size());
            hasChanged = true;
        }

        // ======= 3. GỬI MAIL NẾU CÓ THAY ĐỔI =======
        if (hasChanged) {
            sendWorkScheduleChangedEmail(staff);
        }

        // ======= 4. TRẢ VỀ RESPONSE =======
        return toCreate.stream()
                .map(s -> WorkScheduleDetailResponse.builder()
                        .id(s.getId())
                        .shiftDate(s.getShiftDate())
                        .shift(shiftMapper.toResponse(s.getShift()))
                        .staffId(staffId)
                        .staffName(staff.getFullName())
                        .status(s.getStatus())
                        .note(s.getNote())
                        .build())
                .toList();
    }

    private void validateNewSchedules(String staffId, List<UpdateWorkScheduleRequest> newSchedules) {
        if (newSchedules == null || newSchedules.isEmpty()) return;

        // 1. Kiểm tra trùng lịch trong request
        Set<String> inputKeys = new HashSet<>();
        for (UpdateWorkScheduleRequest item : newSchedules) {
            String key = item.getShiftDate() + "_" + item.getShiftId();
            if (!inputKeys.add(key)) {
                throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS,
                        String.format("Trùng lịch trong yêu cầu tạo mới: ngày [%s] - ca [%s]",
                                item.getShiftDate(), item.getShiftId()));
            }

            // 2. Kiểm tra lịch quá khứ
            if (item.getShiftDate().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.CANNOT_CREATE_PAST_SCHEDULE,
                        String.format("Không thể tạo lịch làm việc trong quá khứ: [%s]", item.getShiftDate()));
            }
        }

        // 3. Kiểm tra trùng lịch trong DB
        List<LocalDate> shiftDates = newSchedules.stream().map(UpdateWorkScheduleRequest::getShiftDate).distinct().toList();
        List<String> shiftIds = newSchedules.stream().map(UpdateWorkScheduleRequest::getShiftId).distinct().toList();

        List<WorkSchedule> existing = workScheduleRepository
                .findAllByStaffIdAndShiftDateInAndShiftIdIn(staffId, shiftDates, shiftIds);
        Set<String> existingKeys = existing.stream()
                .map(ws -> ws.getShiftDate() + "_" + ws.getShift().getId())
                .collect(Collectors.toSet());

        for (UpdateWorkScheduleRequest item : newSchedules) {
            String key = item.getShiftDate() + "_" + item.getShiftId();
            if (existingKeys.contains(key)) {
                throw new AppException(ErrorCode.WORK_SCHEDULE_ALREADY_EXISTS,
                        String.format("Đã tồn tại lịch làm việc: ngày [%s] - ca [%s]", item.getShiftDate(), item.getShiftId()));
            }
        }

        // 4. Kiểm tra shift tồn tại
        List<String> missingShiftIds = shiftIds.stream()
                .filter(id -> !shiftRepository.existsById(id))
                .toList();
        if (!missingShiftIds.isEmpty()) {
            throw new AppException(ErrorCode.SHIFT_NOT_FOUND,
                    "Không tìm thấy các ca làm: " + String.join(", ", missingShiftIds));
        }
    }


    @Override
    public boolean isStaffOnShiftNow(String staffId) {
        final int EARLY_LIMIT = 15; // phút cho phép sớm

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime now = LocalDateTime.now();

        // Lấy schedule hôm qua và hôm nay để bao phủ ca qua đêm
        List<WorkSchedule> schedules = workScheduleRepository
                .findAllByStaff_IdAndShiftDateBetweenAndDeletedAtIsNull(staffId, yesterday, today);

        return schedules.stream().anyMatch(schedule -> {
            // Bỏ qua nếu đang nghỉ phép
            if (schedule.getStatus() == WorkStatus.ON_LEAVE) {
                return false;
            }

            LocalDate shiftDate = schedule.getShiftDate();
            LocalTime start = schedule.getShift().getStartTime();
            LocalTime end   = schedule.getShift().getEndTime();
            boolean overnight = end.isBefore(start);

            LocalDateTime startDT = LocalDateTime.of(shiftDate, start);
            LocalDateTime endDT   = overnight
                    ? LocalDateTime.of(shiftDate.plusDays(1), end)
                    : LocalDateTime.of(shiftDate, end);

            // Cho phép sớm tối đa EARLY_LIMIT phút
            LocalDateTime earliest = startDT.minusMinutes(EARLY_LIMIT);

            return !now.isBefore(earliest) && !now.isAfter(endDT);
        });
    }

    @Override
    public long countShiftsToday(String departmentId) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        return workScheduleRepository.countByStaff_Department_IdAndShiftDateAndStatusIn(
                departmentId, today, java.util.List.of(WorkStatus.SCHEDULED, WorkStatus.ATTENDED)
        );
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
