package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.DailyQueueRepository;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentRepository;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.repository.QueuePatientsRepository;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePollingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueuePatientsServiceImpl implements QueuePatientsService {

    private final QueuePatientsRepository queuePatientsRepository;
    private final QueuePatientsMapper queuePatientsMapper;
    private final DailyQueueService dailyQueueService;
    private final DailyQueueRepository dailyQueueRepository;
    private final PatientRepository patientRepository;
    private final CallbackRegistry callbackRegistry;
    private final QueuePollingService queuePollingService;
    private final DepartmentRepository departmentRepository;

    /**
     * Tạo mới lượt khám cho bệnh nhân.
     * Nếu truyền vào roomNumber hoặc queueOrder → đánh dấu là lượt khám ưu tiên
     */
    @Transactional
    @Override
    public QueuePatientsResponse createQueuePatients(QueuePatientsRequest request) {
        // 1. Kiểm tra thời gian đăng ký không được null
        LocalDateTime registeredTime = request.getRegisteredTime();

        // 2. Nếu có chỉ định phòng → xác thực phòng có tồn tại và thuộc đúng loại khoa
        if (request.getRoomNumber() != null) {
            boolean roomValid = departmentRepository
                    .findByTypeAndRoomNumber(request.getType().name(), request.getRoomNumber())
                    .isPresent();
            if (!roomValid) {
                throw new AppException(ErrorCode.INVALID_ROOM_FOR_DEPARTMENT);
            }
        }

        // 3. Tìm thông tin bệnh nhân từ DB
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        // 4. Xác định queueId tương ứng với ngày đăng ký (hôm nay hoặc tương lai)
        String queueId = resolveQueueId(registeredTime);

        // 5. Kiểm tra bệnh nhân đã có lượt khám chưa hoàn tất trong hàng đợi này chưa
        if (queuePatientsRepository.countActiveVisits(queueId, patient.getId()) > 0) {
            throw new AppException(ErrorCode.ALREADY_IN_QUEUE);
        }

        // 6. Đánh dấu ưu tiên nếu là đặt trước hoặc có chỉ định phòng
        boolean isFutureBooking = registeredTime.toLocalDate().isAfter(LocalDate.now());
        boolean isManualRoomAssigned = request.getRoomNumber() != null;
        boolean isPriority = isFutureBooking || isManualRoomAssigned;

        // 7. Khởi tạo thông tin lượt khám mới
        QueuePatients queuePatient = QueuePatients.builder()
                .queueId(queueId)
                .patientId(patient.getId())
                .type(request.getType())
                .status(Status.WAITING.name())
                .isPriority(isPriority)
                .roomNumber(request.getRoomNumber())
                .registeredTime(request.getRegisteredTime())
                .build();

        // 8. Lưu lượt khám mới vào DB
        QueuePatients saved = queuePatientsRepository.save(queuePatient);

        // 9. Đăng ký callback để đẩy thông báo realtime nếu cần
        callbackRegistry.register(saved.getPatientId());

        // 10. Trả về thông tin lượt khám mới
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
        List<String> statuses = List.of(Status.WAITING.name(), Status.IN_PROGRESS.name());
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

}
