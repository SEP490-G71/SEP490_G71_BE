package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientCompactResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.*;
import vn.edu.fpt.medicaldiagnosis.specification.PatientSpecification;
import vn.edu.fpt.medicaldiagnosis.specification.QueuePatientsSpecification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueuePatientsServiceImpl implements QueuePatientsService {

    private final QueuePatientsRepository queuePatientsRepository;
    private final QueuePatientsMapper queuePatientsMapper;
    private final DailyQueueService dailyQueueService;
    private final StaffRepository staffRepository;
    private final SpecializationRepository specializationRepository;
    private final DailyQueueRepository dailyQueueRepository;
    private final PatientRepository patientRepository;
    private final CallbackRegistry callbackRegistry;
    private final QueuePollingService queuePollingService;
    private final DepartmentRepository departmentRepository;
    private final AccountRepository accountRepository;
    private final WorkScheduleService workScheduleService;
    /**
     * Tạo mới lượt khám cho bệnh nhân.
     * Nếu truyền vào roomNumber hoặc queueOrder → đánh dấu là lượt khám ưu tiên
     */
    @Transactional
    @Override
    public QueuePatientsResponse createQueuePatients(QueuePatientsRequest request) {

        // 0. Kiem tra thong tin request
        DepartmentType type = request.getType() != null ? request.getType() : DepartmentType.CONSULTATION;

        // 1. Lấy thời gian đăng ký từ request (không được null)
        LocalDateTime registeredTime = request.getRegisteredTime();

        // 2. Lấy chuyên khoa (nếu có), nếu không tìm thấy thì ném lỗi
        Specialization specialization = null;
        if (request.getSpecializationId() != null) {
            specialization = specializationRepository.findById(request.getSpecializationId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));
        }

        // 3. Kiểm tra loại khoa có hợp lệ với chuyên khoa đã chọn hay không
        boolean isValidRoom = departmentRepository
                .findByTypeAndSpecializationId(type.name(), request.getSpecializationId())
                .isPresent();
        if (!isValidRoom) {
            throw new AppException(ErrorCode.INVALID_ROOM_FOR_DEPARTMENT);
        }

        // Kiểm tra số lượng phòng chưa quá tải theo specializationId
        int availableRooms = departmentRepository.countAvailableRoomsBySpecialization(request.getSpecializationId());
        if (availableRooms == 0) {
            throw new AppException(ErrorCode.ROOMS_OVERLOADED);
        }

        // 4. Nếu có chỉ định phòng → kiểm tra phòng có tồn tại, đúng loại khoa và đúng chuyên khoa
        if (request.getRoomNumber() != null) {
            boolean roomValid = departmentRepository
                    .findByTypeAndRoomNumberAndSpecializationId(
                            type.name(),
                            request.getRoomNumber(),
                            request.getSpecializationId()
                    )
                    .isPresent();
            if (!roomValid) {
                throw new AppException(ErrorCode.INVALID_ROOM_FOR_DEPARTMENT);
            }
        }

        // 5. Tìm thông tin bệnh nhân theo ID (không bị xoá mềm)
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        // 6. Tạo queueId tương ứng với ngày đăng ký (xếp theo ngày)
        String queueId = resolveQueueId(registeredTime);

        // 7. Kiểm tra bệnh nhân đã có lượt khám chưa hoàn tất trong cùng queueId chưa
        if (queuePatientsRepository.countActiveVisits(queueId, patient.getId()) > 0) {
            throw new AppException(ErrorCode.ALREADY_IN_QUEUE);
        }

        // 8. Xác định có phải lượt ưu tiên hay không:
        // - Ưu tiên nếu đăng ký cho ngày tương lai
        boolean isPriority = registeredTime.toLocalDate().isAfter(LocalDate.now());

        // 9. Xác định người dùng hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Người dùng hiện tại: {}", username);

        // 10. Lấy tài khoản và thông tin nhân viên
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));

        Staff currentStaff = staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));

        if (!workScheduleService.isStaffOnShiftNow(currentStaff.getId())) {
            throw new AppException(ErrorCode.ACTION_NOT_ALLOWED, "không trong ca làm không thể thao tác");
        }

        QueuePatients queuePatient = QueuePatients.builder()
                .queueId(queueId)
                .patientId(patient.getId())
                .type(type)
                .status(Status.WAITING.name())
                .isPriority(request.getIsPriority() != null ? request.getIsPriority() : isPriority)
                .roomNumber(request.getRoomNumber())
                .registeredTime(registeredTime)
                .specialization(specialization)
                .build();

        // 11. Lưu thông tin lượt khám vào cơ sở dữ liệu
        QueuePatients saved = queuePatientsRepository.save(queuePatient);

        // 12. Đăng ký callback theo dõi thay đổi để hỗ trợ realtime update (nếu có)
        callbackRegistry.register(saved.getPatientId());

        // 13. Trả về response DTO
        return queuePatientsMapper.toResponse(saved);
    }

    private String resolveQueueId(LocalDateTime registeredTime) {
        if (registeredTime.toLocalDate().isEqual(LocalDate.now())) {
            return Optional.ofNullable(dailyQueueService.getActiveQueueIdForToday())
                    .orElseThrow(() -> new AppException(ErrorCode.QUEUE_NOT_FOUND));
        }

        if (registeredTime.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_QUEUE_DATE);
        }

        LocalDateTime queueDateTime = registeredTime.toLocalDate().atTime(7, 0);
        return dailyQueueRepository.findByQueueDateAndDeletedAtIsNull(queueDateTime)
                .map(DailyQueue::getId)
                .orElseGet(() -> {
                    DailyQueue newQueue = DailyQueue.builder()
                            .queueDate(queueDateTime)
                            .status(Status.INACTIVE.name())
                            .build();
                    return dailyQueueRepository.save(newQueue).getId();
                });
    }

    /**
     * Cập nhật thông tin lượt khám (status, queueOrder, thời gian,...)
     */
    @Override
    public QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        // Cập nhật thứ tự nếu được truyền vào
        if (request.getQueueOrder() != null) {
            entity.setQueueOrder(request.getQueueOrder());
        }

        // Cập nhật trạng thái nếu thay đổi (không cho rollback trạng thái như DONE → WAITING)
        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            String oldStatus = entity.getStatus();

            if (!Status.valueOf(newStatus).equals(Status.valueOf(oldStatus))) {
                entity.setStatus(newStatus);
                log.info("Chuyển trạng thái bệnh nhân {} từ {} → {}", entity.getPatientId(), oldStatus, newStatus);
            }
        }

        // Ghi nhận thời gian checkin (vào phòng khám)
        if (request.getCheckinTime() != null) {
            entity.setCheckinTime(request.getCheckinTime());
            log.info("Cập nhật checkinTime bệnh nhân {} vào {}", entity.getPatientId(), request.getCheckinTime());
        }

        // Ghi nhận thời gian checkout (rời phòng)
        if (request.getCheckoutTime() != null) {
            entity.setCheckoutTime(request.getCheckoutTime());
            log.info("Cập nhật checkoutTime bệnh nhân {} thành {}", entity.getPatientId(), request.getCheckoutTime());
        }

        // Cập nhật phòng khám nếu có thay đổi
        if (request.getRoomNumber() != null) {
            entity.setRoomNumber(request.getRoomNumber());
        }

        // Ghi nhận thời điểm bắt đầu được gọi khám
        if (request.getCalledTime() != null) {
            entity.setCalledTime(request.getCalledTime());
            log.info("Cập nhật calledTime bệnh nhân {} vào {}", entity.getPatientId(), request.getCalledTime());
        }

        if (request.getAwaitingResultTime() != null) {
            entity.setAwaitingResultTime(request.getAwaitingResultTime());
            log.info("Cập nhật awaitingResultTime bệnh nhân {} vào {}", entity.getPatientId(), request.getAwaitingResultTime());
        }

        if (request.getMessage() != null) {
            entity.setMessage(request.getMessage());
            log.info("Cập nhật message bệnh nhân {} vào {}", entity.getPatientId(), request.getMessage());
        }

        QueuePatients updated = queuePatientsRepository.save(entity);

        queuePollingService.notifyListeners(getAllQueuePatients());
        return queuePatientsMapper.toResponse(updated);
    }

    /**
     * Xoá mềm lượt khám (đặt deletedAt = now)
     */
    @Override
    public void deleteQueuePatients(String id) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        entity.setDeletedAt(LocalDateTime.now());
        queuePatientsRepository.save(entity);

        log.info("Đã soft delete bệnh nhân {}", entity.getPatientId());
    }

    /**
     * Lấy thông tin chi tiết một lượt khám
     */
    @Override
    public QueuePatientsResponse getQueuePatientsById(String id) {
        return queuePatientsMapper.toResponse(queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND)));
    }

    /**
     * Lấy toàn bộ danh sách bệnh nhân của queue hiện tại trong ngày
     */
    @Override
    public List<QueuePatientsResponse> getAllQueuePatients() {
        String todayQueueId = dailyQueueService.getActiveQueueIdForToday();
        if (todayQueueId == null) {
            throw new AppException(ErrorCode.QUEUE_NOT_FOUND);
        }

        return queuePatientsRepository.findAllByQueueId(todayQueueId)
                .stream()
                .map(queuePatient -> {
                    QueuePatientsResponse response = queuePatientsMapper.toResponse(queuePatient);

                    // Gọi sang PatientService để lấy fullName
                    Optional<Patient> patientOpt = patientRepository.findByIdAndDeletedAtIsNull(queuePatient.getPatientId());
                    patientOpt.ifPresent(patient -> {
                        response.setFullName(patient.getFullNameSafe());
                    });

                    return response;
                })
                .collect(Collectors.toList());
    }


    /**
     * Lấy danh sách bệnh nhân theo status và queueId cụ thể
     */
    @Override
    public List<QueuePatientsResponse> getAllQueuePatientsByStatusAndQueueId(String status, String queueId) {
        return queuePatientsRepository.findAllByStatusAndQueueId(status, queueId)
                .stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy queueOrder lớn nhất trong một phòng khám
     */
    @Transactional
    @Override
    public Long getMaxQueueOrderForRoom(String roomNumber, String queueId) {
        Long max = queuePatientsRepository.findMaxQueueOrderByRoom(roomNumber, queueId);
        return (max != null) ? max : 0L;
    }

    /**
     * Lấy danh sách bệnh nhân chưa được phân phòng và không phải ưu tiên
     */
    @Override
    public List<QueuePatientsResponse> getTopWaitingUnassigned(String queueId, int limit) {
        return queuePatientsRepository.findTopUnassignedWaiting(queueId, limit).stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách bệnh nhân đã được gán vào một phòng cụ thể (WAITING hoặc IN_PROGRESS)
     */
    @Override
    public List<QueuePatientsResponse> getAssignedPatientsForRoom(String queueId, String roomNumber) {
        List<String> statuses = List.of(Status.WAITING.name(), Status.CALLING.name(), Status.IN_PROGRESS.name());
        return queuePatientsRepository.findAssigned(queueId, roomNumber, statuses)
                .stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật atomically room + queueOrder cho một bệnh nhân nếu chưa có phòng
     */
    @Transactional
    @Override
    public boolean tryAssignPatientToRoom(String patientId, int roomNumber, long queueOrder) {
        // Gọi repository để thực hiện update có điều kiện
        int updated = queuePatientsRepository.tryAssignRoom(
                patientId,
                String.valueOf(roomNumber),
                queueOrder
        );

        // updated = 1 nếu thành công, = 0 nếu bệnh nhân đã được gán phòng từ trước
        return updated > 0;
    }

    @Override
    public List<QueuePatientsResponse> getTopWaitingPriority(String queueId, int limit) {
        return queuePatientsRepository.findTopPriorityWaiting(queueId, limit).stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<QueuePatientCompactResponse> searchQueuePatients(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        Sort baseSort = Sort.by(
                Sort.Order.desc("isPriority"),
                Sort.Order.asc("queueOrder")
        );

        Sort userSort = Sort.by(sortBy);
        userSort = "asc".equalsIgnoreCase(sortDir) ? userSort.ascending() : userSort.descending();

        Sort sort = baseSort.and(userSort);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 1. Tách filters
        Map<String, String> patientFilters = new HashMap<>();
        Map<String, String> queueFilters = new HashMap<>();

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) continue;

            if (List.of("name", "gender", "phone", "patientCode").contains(key)) {
                patientFilters.put(key, value);
            } else {
                queueFilters.put(key, value);
            }
        }

        // 2. Nếu có filter bệnh nhân → lọc trước, lấy danh sách patientId
        Specification<Patient> patientSpec = PatientSpecification.buildSpecification(patientFilters);
        List<String> patientIds = null;

        if (!patientFilters.isEmpty()) {
            patientIds = patientRepository.findAll(patientSpec).stream()
                    .map(Patient::getId)
                    .toList();

            if (patientIds.isEmpty()) {
                return Page.empty();
            }
        }

        // 3. Build QueuePatientsSpecification và thêm điều kiện patient_id in (...)
        Specification<QueuePatients> queueSpec = QueuePatientsSpecification.buildSpecification(queueFilters);

        if (patientIds != null) {
            List<String> finalPatientIds = patientIds;
            queueSpec = queueSpec.and((root, query, cb) -> root.get("patientId").in(finalPatientIds));
        }

        // 4. Query queue patients
        Page<QueuePatients> queuePage = queuePatientsRepository.findAll(queueSpec, pageable);

        // 5. Load bệnh nhân tương ứng
        Map<String, Patient> patientMap = patientRepository.findAllById(
                queuePage.stream()
                        .map(QueuePatients::getPatientId)
                        .toList()
        ).stream().collect(Collectors.toMap(Patient::getId, Function.identity()));

        // 6. Map thủ công sang QueuePatientCompactResponse
        return queuePage.map(qp -> {
            Patient patient = patientMap.get(qp.getPatientId());
            return queuePatientsMapper.toCompactResponse(qp, patient);
        });
    }

    @Override
    public QueuePatientCompactResponse getQueuePatientDetail(String id) {
        log.info("Service: get queue patient by id {}", id);

        QueuePatients queuePatient = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(
                        ErrorCode.QUEUE_PATIENT_NOT_FOUND,
                        "Không tìm thấy queue patient với id = " + id
                ));

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(queuePatient.getPatientId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.PATIENT_NOT_FOUND,
                        "Không tìm thấy bệnh nhân với id = " + queuePatient.getPatientId() + " (từ queueId = " + id + ")"
                ));

        return queuePatientsMapper.toCompactResponse(queuePatient, patient);
    }

    @Override
    @Transactional
    public QueuePatientsResponse updateQueuePatientStatus(String id, String newStatus) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        // 1) Xác định người dùng hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Người dùng hiện tại: {}", username);

        // 2) Lấy account + staff + check ca làm
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));

        Staff staff = staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));

        if (!workScheduleService.isStaffOnShiftNow(staff.getId())) {
            throw new AppException(ErrorCode.ACTION_NOT_ALLOWED, "không trong ca làm không thể thao tác");
        }

        String oldStatus = entity.getStatus();

        // Không cho cập nhật nếu đã DONE
        if (Status.DONE.name().equalsIgnoreCase(oldStatus)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Không thay đổi trạng thái thì bỏ qua
        if (Status.valueOf(newStatus).name().equalsIgnoreCase(oldStatus)) {
            return queuePatientsMapper.toResponse(entity);
        }

        // Nếu chuyển từ WAITING → CALLING thì cần kiểm tra thứ tự
        if (Status.WAITING.name().equalsIgnoreCase(oldStatus)
                && Status.CALLING.name().equalsIgnoreCase(newStatus)) {
            ensureQueueOrderIsValid(entity);
        }

        // --- set status + tách side-effects theo trạng thái ---
        entity.setStatus(newStatus);
        applyStatusUpdates(entity, Status.valueOf(newStatus));
        // --------------------------------------------------------------------------

        log.info("Chuyển trạng thái bệnh nhân {} từ {} → {}", entity.getPatientId(), oldStatus, newStatus);

        queuePatientsRepository.save(entity);
        queuePollingService.notifyListeners(getAllQueuePatients());

        return queuePatientsMapper.toResponse(entity);
    }

    private void ensureQueueOrderIsValid(QueuePatients queuePatients) {
        String queueId = queuePatients.getQueueId();
        String roomNumber = queuePatients.getRoomNumber();
        Long queueOrder = queuePatients.getQueueOrder();

        if (queueId != null && roomNumber != null && queueOrder != null) {
            boolean blockCalling;
            if (Boolean.TRUE.equals(queuePatients.getIsPriority())) {
                Long count = queuePatientsRepository
                        .countPriorityPatientBefore(queueId, roomNumber, queueOrder);
                blockCalling = count != null && count > 0;
            } else {
                Long count = queuePatientsRepository
                        .countEarlierPatientBlocking(queueId, roomNumber, queueOrder);
                blockCalling = count != null && count > 0;
            }
            if (blockCalling) {
                throw new AppException(ErrorCode.INVALID_QUEUE_ORDER);
            }
        }
    }

    /** Cập nhật các field phụ theo trạng thái mới */
    private void applyStatusUpdates(QueuePatients e, Status newStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case CALLING -> {
                if (e.getCalledTime() == null) e.setCalledTime(now);
                // if (e.getMessage() == null) e.setMessage(buildCallingMessage(e));
            }
            case IN_PROGRESS -> {
                if (e.getCheckinTime() == null) e.setCheckinTime(now);
            }
            case AWAITING_RESULT -> {
                if (e.getAwaitingResultTime() == null) e.setAwaitingResultTime(now);
            }
            case DONE -> {
                if (e.getCheckoutTime() == null) e.setCheckoutTime(now);
            }
            case CANCELED, WAITING -> {
                // không side-effects
            }
            default -> { /* no-op */ }
        }
    }

    @Override
    public long countActivePatientsInRoom(String roomNumber, String queueId) {
        List<String> activeStatuses = List.of(
                Status.WAITING.name(),
                Status.CALLING.name(),
                Status.IN_PROGRESS.name(),
                Status.AWAITING_RESULT.name()
        );
        return queuePatientsRepository.countByRoomNumberAndQueueIdAndStatusIn(
                roomNumber, queueId, activeStatuses
        );
    }


}
