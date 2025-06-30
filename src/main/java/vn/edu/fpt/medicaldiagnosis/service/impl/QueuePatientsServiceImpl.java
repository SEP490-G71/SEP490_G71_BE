package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.repository.QueuePatientsRepository;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePollingService;

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
    private final PatientRepository patientRepository;
    private final CallbackRegistry callbackRegistry;
    private final QueuePollingService queuePollingService;

    /**
     * Tạo mới lượt khám cho bệnh nhân.
     * Nếu truyền vào roomNumber hoặc queueOrder → đánh dấu là lượt khám ưu tiên
     */
    @Override
    public QueuePatientsResponse createQueuePatients(QueuePatientsRequest request) {

        if (request.getRegisteredTime() == null) {
            throw new AppException(ErrorCode.REGISTERED_TIME_REQUIRED);
        }

        String todayQueueId = dailyQueueService.getActiveQueueIdForToday();
        if (todayQueueId == null) {
            throw new AppException(ErrorCode.QUEUE_NOT_FOUND);
        }

        if (request.getType() == null) {
            throw new AppException(ErrorCode.DEPARTMENT_TYPE_EMPTY);
        }

        if (request.getPatientId() == null) {
            throw new AppException(ErrorCode.PATIENT_ID_REQUIRED);
        }

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        // Không cho phép đăng ký nếu bệnh nhân đã có lượt khám chưa hoàn tất
        int activeVisitCount = queuePatientsRepository.countActiveVisits(todayQueueId, patient.getId());
        if (activeVisitCount > 0) {
            throw new AppException(ErrorCode.ALREADY_IN_QUEUE);
        }

        // Khởi tạo builder cơ bản
        QueuePatients.QueuePatientsBuilder builder = QueuePatients.builder()
                .queueId(todayQueueId)
                .patientId(patient.getId())
                .type(request.getType())
                .isPriority(false)
                .status(Status.WAITING.name());

        // Nếu có chỉ định room → đánh dấu là ưu tiên
        if (request.getRoomNumber() != null) {
            builder.isPriority(true);
            builder.roomNumber(request.getRoomNumber());
        }

        QueuePatients saved = queuePatientsRepository.save(builder.build());

        callbackRegistry.register(saved.getPatientId());

        return queuePatientsMapper.toResponse(saved);
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
