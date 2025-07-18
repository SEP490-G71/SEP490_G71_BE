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
import vn.edu.fpt.medicaldiagnosis.dto.request.LeaveRequestDetailDTO;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateLeaveRequestStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.LeaveRequestResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.LeaveRequestStatus;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.LeaveRequestRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.LeaveRequestService;
import vn.edu.fpt.medicaldiagnosis.specification.LeaveRequestSpecification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    @Override
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        log.info("Service: create leave request");
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // Validate từng detail
        for (LeaveRequestDetailDTO detail : request.getDetails()) {
            LocalDate date = detail.getDate();
            Shift shift = detail.getShift();

            // Không cho nghỉ trong quá khứ
            if (date.isBefore(today)) {
                throw new AppException(ErrorCode.CANNOT_REQUEST_LEAVE_FOR_PAST_SCHEDULE);
            }

            // Không cho nghỉ nếu không trước ít nhất 2 ngày
            if (!date.isAfter(today.plusDays(1))) {
                throw new AppException(ErrorCode.LEAVE_REQUEST_TOO_CLOSE);
            }

            // Kiểm tra xem lịch làm có tồn tại không
            Optional<WorkSchedule> optionalSchedule = workScheduleRepository
                    .findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(
                            staff.getId(), date, shift
                    );

            if (optionalSchedule.isEmpty()) {
                throw new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND_FOR_LEAVE);
            }

            // Nếu có tồn tại thì check ATTENDED
            if (WorkStatus.ATTENDED.equals(optionalSchedule.get().getStatus())) {
                throw new AppException(ErrorCode.WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE);
            }
        }

        // Tạo entity
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(UUID.randomUUID().toString());
        leaveRequest.setStaff(staff);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        List<LeaveRequestDetail> details = request.getDetails().stream()
                .map(dto -> LeaveRequestDetail.builder()
                        .id(UUID.randomUUID().toString())
                        .leaveRequest(leaveRequest)
                        .date(dto.getDate())
                        .shift(dto.getShift())
                        .build()
                ).toList();

        leaveRequest.setDetails(details);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .staffName(staff.getFullName())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
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
            throw new AppException(ErrorCode.LEAVE_REQUEST_ALREADY_PROCESSED);
        }

        if (leaveRequest.getCreatedAt().toLocalDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.LEAVE_REQUEST_CREATE_DATE_INVALID);
        }

        // Nếu duyệt, phải check các ca hợp lệ và cập nhật trạng thái
        if (request.getStatus() == LeaveRequestStatus.APPROVED) {
            for (LeaveRequestDetail detail : leaveRequest.getDetails()) {
                LocalDate date = detail.getDate();

                // Tìm lịch làm tương ứng để cập nhật trạng thái thành nghỉ phép
                WorkSchedule schedule = workScheduleRepository.findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(
                        leaveRequest.getStaff().getId(), date, detail.getShift()
                ).orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND_FOR_LEAVE));

                if (schedule.getStatus() == WorkStatus.ATTENDED) {
                    throw new AppException(ErrorCode.CANNOT_APPROVE_LEAVE_FOR_ATTENDED_SHIFT);
                }

                schedule.setStatus(WorkStatus.ON_LEAVE);
                workScheduleRepository.save(schedule);
            }

            leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        } else if (request.getStatus() == LeaveRequestStatus.REJECTED) {
            leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        } else {
            throw new AppException(ErrorCode.INVALID_LEAVE_STATUS);
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
                                .map(d -> new LeaveRequestDetailDTO(d.getDate(), d.getShift()))
                                .toList()
                )
                .build();
    }

    @Override
    public void deleteLeaveRequest(String leaveRequestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndDeletedAtIsNull(leaveRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));

        if (!LeaveRequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_PROCESSED_LEAVE_REQUEST);
        }

        leaveRequestRepository.delete(leaveRequest);
    }

    @Override
    public LeaveRequestResponse updateLeaveRequest(String leaveRequestId, CreateLeaveRequest request) {
        log.info("Service: update leave request {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndDeletedAtIsNull(leaveRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));

        if (!LeaveRequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_PROCESSED_LEAVE_REQUEST);
        }

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // ✅ Validate từng ca xin nghỉ
        for (LeaveRequestDetailDTO detail : request.getDetails()) {
            LocalDate date = detail.getDate();
            Shift shift = detail.getShift();

            // Không cho nghỉ trong quá khứ
            if (date.isBefore(today)) {
                throw new AppException(ErrorCode.CANNOT_REQUEST_LEAVE_FOR_PAST_SCHEDULE);
            }

            // Không cho nghỉ nếu không trước ít nhất 2 ngày
            if (!date.isAfter(today.plusDays(1))) {
                throw new AppException(ErrorCode.LEAVE_REQUEST_TOO_CLOSE);
            }

            // Kiểm tra xem lịch làm có tồn tại không
            Optional<WorkSchedule> optionalSchedule = workScheduleRepository
                    .findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(staff.getId(), date, shift);

            if (optionalSchedule.isEmpty()) {
                throw new AppException(ErrorCode.WORK_SCHEDULE_NOT_FOUND_FOR_LEAVE);
            }

            if (WorkStatus.ATTENDED.equals(optionalSchedule.get().getStatus())) {
                throw new AppException(ErrorCode.WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE);
            }
        }

        // Xoá chi tiết cũ (vì có orphanRemoval = true, set lại list là được)
        List<LeaveRequestDetail> details = request.getDetails().stream()
                .map(dto -> LeaveRequestDetail.builder()
                        .id(UUID.randomUUID().toString())
                        .leaveRequest(leaveRequest)
                        .date(dto.getDate())
                        .shift(dto.getShift())
                        .build()
                )
                .collect(Collectors.toList()); // danh sách có thể thay đổi


        leaveRequest.setReason(request.getReason());
        // Xóa và cập nhật danh sách details đúng cách
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
                                .map(d -> new LeaveRequestDetailDTO(d.getDate(), d.getShift()))
                                .toList()
                )
                .build()
        );
    }



}
