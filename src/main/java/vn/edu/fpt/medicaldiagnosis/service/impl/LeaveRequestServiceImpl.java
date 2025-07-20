package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.CreateLeaveRequestByTime;
import vn.edu.fpt.medicaldiagnosis.dto.request.LeaveRequestDetailDTO;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.LeaveRequestStatus;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.LeaveRequestRepository;
import vn.edu.fpt.medicaldiagnosis.repository.ShiftRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.LeaveRequestService;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;
import vn.edu.fpt.medicaldiagnosis.specification.LeaveRequestSpecification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {
    StaffRepository staffRepository;
    LeaveRequestRepository leaveRequestRepository;
    WorkScheduleRepository workScheduleRepository;
    ShiftRepository shiftRepository;
    SettingService settingService;
    @Override
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        log.info("Service: create leave request");

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        LocalDate today = LocalDate.now();
        // Lấy số ngày tối thiểu phải xin nghỉ trước
        SettingResponse setting = settingService.getSetting();
        Integer minLeaveDaysBefore = setting.getMinLeaveDaysBefore();
        // Danh sách chi tiết đã chuẩn hóa với shift entity
        List<LeaveRequestDetail> details = new ArrayList<>();

        for (LeaveRequestDetailDTO detail : request.getDetails()) {
            LocalDate date = detail.getDate();
            String shiftId = detail.getShiftId();

            // Không cho nghỉ trong quá khứ
            if (date.isBefore(today)) {
                throw new AppException(ErrorCode.CANNOT_REQUEST_LEAVE_FOR_PAST_SCHEDULE,
                        "Không thể tạo đơn xin nghỉ cho ngày trong quá khứ: " + date);
            }

            // Kiểm tra số ngày tối thiểu phải xin nghỉ trước
            if (!date.isAfter(today.plusDays(minLeaveDaysBefore - 1))) {
                throw new AppException(ErrorCode.LEAVE_REQUEST_TOO_CLOSE,
                        "Ngày xin nghỉ phải trước ít nhất " + minLeaveDaysBefore + " ngày. Ngày chọn: " + date);
            }

            // Lấy shift từ DB
            Shift shift = shiftRepository.findByIdAndDeletedAtIsNull(shiftId)
                    .orElseThrow(() -> new AppException(ErrorCode.SHIFT_NOT_FOUND));

            // Kiểm tra xem lịch làm có tồn tại không
            Optional<WorkSchedule> optionalSchedule = workScheduleRepository
                    .findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(staff.getId(), date, shift);

            if (optionalSchedule.isEmpty()) {
                throw new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND_FOR_LEAVE,
                        "Không tìm thấy lịch làm việc cho nhân viên [" + staff.getFullName() + "] ngày " + date + ", ca " + shift.getName());
            }

            if (WorkStatus.ATTENDED.equals(optionalSchedule.get().getStatus())) {
                throw new AppException(ErrorCode.WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE,
                        "Ca làm ngày " + date + ", ca " + shift.getName() + " đã điểm danh. Không thể xin nghỉ.");
            }

            // Add detail vào list
            LeaveRequestDetail leaveDetail = LeaveRequestDetail.builder()
                    .id(UUID.randomUUID().toString())
                    .leaveRequest(null) // gán sau
                    .date(date)
                    .shift(shift)
                    .build();
            details.add(leaveDetail);
        }

        // Tạo leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(UUID.randomUUID().toString());
        leaveRequest.setStaff(staff);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        // Gán leaveRequest vào từng detail
        for (LeaveRequestDetail d : details) {
            d.setLeaveRequest(leaveRequest);
        }

        leaveRequest.setDetails(details);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(saved.getId())
                .staffName(staff.getFullName())
                .reason(saved.getReason())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .details(request.getDetails())
                .build();
    }

    @Override
    public LeaveRequestResponse updateLeaveRequestStatus(UpdateLeaveRequestStatusRequest request) {
        log.info("Service: update leave request status");
        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndDeletedAtIsNull(request.getLeaveRequestId())
                .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));

        // Không xử lý lại đơn đã duyệt hoặc bị từ chối
        if (!LeaveRequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new AppException(
                    ErrorCode.LEAVE_REQUEST_ALREADY_PROCESSED,
                    "Đơn xin nghỉ đã được xử lý trước đó. Trạng thái hiện tại: " + leaveRequest.getStatus()
            );
        }

        if (leaveRequest.getCreatedAt().toLocalDate().isBefore(LocalDate.now())) {
            throw new AppException(
                    ErrorCode.LEAVE_REQUEST_CREATE_DATE_INVALID,
                    "Đơn xin nghỉ đã tạo từ ngày trước. Không thể xử lý đơn đã tạo vào: " + leaveRequest.getCreatedAt().toLocalDate()
            );
        }

        // Nếu duyệt, phải check các ca hợp lệ và cập nhật trạng thái
        if (request.getStatus() == LeaveRequestStatus.APPROVED) {
            for (LeaveRequestDetail detail : leaveRequest.getDetails()) {
                LocalDate date = detail.getDate();

                WorkSchedule schedule = workScheduleRepository.findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(
                        leaveRequest.getStaff().getId(), date, detail.getShift()
                ).orElseThrow(() -> new AppException(
                        ErrorCode.SCHEDULE_NOT_FOUND_FOR_LEAVE,
                        "Không tìm thấy lịch làm việc của nhân viên [" + leaveRequest.getStaff().getFullName()
                                + "] vào ngày " + date + ", ca " + detail.getShift().getName()
                ));

                if (schedule.getStatus() == WorkStatus.ATTENDED) {
                    throw new AppException(
                            ErrorCode.CANNOT_APPROVE_LEAVE_FOR_ATTENDED_SHIFT,
                            "Không thể duyệt nghỉ cho ca đã điểm danh vào ngày " + date + ", ca " + detail.getShift().getName()
                    );
                }

                schedule.setStatus(WorkStatus.ON_LEAVE);
                workScheduleRepository.save(schedule);
            }

            leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        } else if (request.getStatus() == LeaveRequestStatus.REJECTED) {
            leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        } else {
            throw new AppException(
                    ErrorCode.INVALID_LEAVE_STATUS,
                    "Trạng thái đơn xin nghỉ không hợp lệ: " + request.getStatus()
            );

        }

        leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .staffName(leaveRequest.getStaff().getFullName())
                .reason(leaveRequest.getReason())
                .createdAt(leaveRequest.getCreatedAt())
                .status(leaveRequest.getStatus())
                .details(
                        leaveRequest.getDetails().stream()
                                .map(d -> new LeaveRequestDetailDTO(d.getDate(), d.getShift().getId()))
                                .toList()
                )
                .build();
    }

    @Override
    public void deleteLeaveRequest(String leaveRequestId) {
        log.info("Service: delete leave request {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndDeletedAtIsNull(leaveRequestId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.LEAVE_REQUEST_NOT_FOUND,
                        "Không tìm thấy đơn xin nghỉ với ID: " + leaveRequestId
                ));

        if (!LeaveRequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new AppException(
                    ErrorCode.CANNOT_DELETE_PROCESSED_LEAVE_REQUEST,
                    "Không thể xoá đơn đã được xử lý. Trạng thái hiện tại: " + leaveRequest.getStatus()
            );
        }

        leaveRequestRepository.delete(leaveRequest);
    }

    @Override
    public LeaveRequestResponse updateLeaveRequest(String leaveRequestId, CreateLeaveRequest request) {
        log.info("Service: update leave request {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndDeletedAtIsNull(leaveRequestId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.LEAVE_REQUEST_NOT_FOUND,
                        "Không tìm thấy đơn xin nghỉ với ID: " + leaveRequestId
                ));

        if (!LeaveRequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new AppException(
                    ErrorCode.CANNOT_UPDATE_PROCESSED_LEAVE_REQUEST,
                    "Không thể cập nhật đơn đã được xử lý. Trạng thái hiện tại: " + leaveRequest.getStatus()
            );
        }

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.STAFF_NOT_FOUND,
                        "Không tìm thấy nhân viên với ID: " + request.getStaffId()
                ));

        LocalDate today = LocalDate.now();
        SettingResponse setting = settingService.getSetting();
        Integer minLeaveDaysBefore = setting.getMinLeaveDaysBefore();

        List<LeaveRequestDetail> details = new ArrayList<>();

        for (LeaveRequestDetailDTO detail : request.getDetails()) {
            LocalDate date = detail.getDate();
            String shiftId = detail.getShiftId();

            if (date.isBefore(today)) {
                throw new AppException(
                        ErrorCode.CANNOT_REQUEST_LEAVE_FOR_PAST_SCHEDULE,
                        "Không thể xin nghỉ cho ngày trong quá khứ: " + date
                );
            }

            if (!date.isAfter(today.plusDays(minLeaveDaysBefore - 1))) {
                throw new AppException(
                        ErrorCode.LEAVE_REQUEST_TOO_CLOSE,
                        "Ngày xin nghỉ (" + date + ") phải trước ít nhất " + minLeaveDaysBefore + " ngày so với hiện tại."
                );
            }

            Shift shift = shiftRepository.findByIdAndDeletedAtIsNull(shiftId)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.SHIFT_NOT_FOUND,
                            "Không tìm thấy ca làm với ID: " + shiftId
                    ));

            Optional<WorkSchedule> optionalSchedule = workScheduleRepository
                    .findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(staff.getId(), date, shift);

            if (optionalSchedule.isEmpty()) {
                throw new AppException(
                        ErrorCode.WORK_SCHEDULE_NOT_FOUND_FOR_LEAVE,
                        "Không tìm thấy lịch làm việc cho nhân viên [" + staff.getFullName() + "] ngày " + date + ", ca " + shift.getName()
                );
            }

            if (WorkStatus.ATTENDED.equals(optionalSchedule.get().getStatus())) {
                throw new AppException(
                        ErrorCode.WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE,
                        "Ca làm ngày " + date + ", ca " + shift.getName() + " đã được điểm danh. Không thể xin nghỉ."
                );
            }

            LeaveRequestDetail detailSave = LeaveRequestDetail.builder()
                    .id(UUID.randomUUID().toString())
                    .leaveRequest(leaveRequest)
                    .date(date)
                    .shift(shift)
                    .build();

            details.add(detailSave);
        }

        // ✅ Cập nhật đơn xin nghỉ
        leaveRequest.setReason(request.getReason());
        leaveRequest.getDetails().clear();
        leaveRequest.getDetails().addAll(details);
        leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .staffName(staff.getFullName())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
                .createdAt(leaveRequest.getCreatedAt())
                .details(request.getDetails())
                .build();
    }


    @Override
    public Page<LeaveRequestResponse> getLeaveRequestsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get leave requests paged");

        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<LeaveRequest> spec = LeaveRequestSpecification.buildSpecification(filters)
                .and((root, query, cb) -> cb.isNull(root.get("staff").get("deletedAt")));
        Page<LeaveRequest> pageResult = leaveRequestRepository.findAll(spec, pageable);

        return pageResult.map(leaveRequest -> LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .staffName(leaveRequest.getStaff().getFullName())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
                .createdAt(leaveRequest.getCreatedAt())
                .details(
                        leaveRequest.getDetails().stream()
                                .map(d -> new LeaveRequestDetailDTO(d.getDate(), d.getShift().getId()))
                                .toList()
                )
                .build()
        );
    }

    @Override
    public LeaveRequestResponse createLeaveRequestByTime(CreateLeaveRequestByTime request) {
        log.info("Service: create leave request by time");

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        LocalDateTime from = request.getFromDateTime();
        LocalDateTime to = request.getToDateTime();

        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE, "Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        List<WorkSchedule> workSchedules = workScheduleRepository
                .findByStaffIdAndDateTimeRange(staff.getId(), from, to);

        if (workSchedules.isEmpty()) {
            throw new AppException(ErrorCode.NO_WORK_SCHEDULE_IN_RANGE, "Không tìm thấy ca làm việc trong khoảng thời gian yêu cầu");
        }

        // Kiểm tra và chuyển đổi sang list detail
        List<LeaveRequestDetail> details = new ArrayList<>();
        for (WorkSchedule schedule : workSchedules) {
            if (WorkStatus.ATTENDED.equals(schedule.getStatus())) {
                throw new AppException(ErrorCode.WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE,
                        "Ca làm ngày " + schedule.getShiftDate() + ", ca " + schedule.getShift().getName() + " đã điểm danh.");
            }

            LeaveRequestDetail detail = LeaveRequestDetail.builder()
                    .id(UUID.randomUUID().toString())
                    .leaveRequest(null)
                    .date(schedule.getShiftDate())
                    .shift(schedule.getShift())
                    .build();
            details.add(detail);
        }

        // Tạo leave request như cũ
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(UUID.randomUUID().toString());
        leaveRequest.setStaff(staff);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        for (LeaveRequestDetail d : details) {
            d.setLeaveRequest(leaveRequest);
        }

        leaveRequest.setDetails(details);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(saved.getId())
                .staffName(staff.getFullName())
                .reason(saved.getReason())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .details(details.stream().map(d ->
                                new LeaveRequestDetailDTO(d.getDate(), d.getShift().getId()))
                        .toList())
                .build();
    }
}
