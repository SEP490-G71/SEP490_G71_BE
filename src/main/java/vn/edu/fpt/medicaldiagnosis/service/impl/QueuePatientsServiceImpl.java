package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.QueuePatientsRepository;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;

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
    private final PatientService patientService;
    private final CallbackRegistry callbackRegistry;

    @Override
    public QueuePatientsResponse createQueuePatients(QueuePatientsRequest request) {
        PatientResponse patient = patientService.getPatientById(request.getPatientId());
        String todayQueueId = dailyQueueService.getActiveQueueIdForToday();

        QueuePatients queue = QueuePatients.builder()
                .queueId(todayQueueId)
                .patientId(patient.getId())
                .status(Status.WAITING.name())
                .checkinTime(LocalDateTime.now())
                .build();

        QueuePatients saved = queuePatientsRepository.save(queue);

        // Lưu lại callback
        if (request.getCallbackUrl() != null) {
            callbackRegistry.register(saved.getPatientId(), request.getCallbackUrl());
        }

        return queuePatientsMapper.toResponse(saved);
    }

    @Override
    public QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        if (Status.DONE.name().equalsIgnoreCase(entity.getStatus())) {
            throw new AppException(ErrorCode.QUEUE_PATIENT_ALREADY_FINISHED);
        }

        // Chỉ cho phép cập nhật queueOrder, status, checkoutTime
        if (request.getQueueOrder() != null) {
            entity.setQueueOrder(request.getQueueOrder());
        }

        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            String oldStatus = entity.getStatus();

            // Không cho phép lùi trạng thái (ví dụ: DONE -> WAITING)
            if (!Status.valueOf(newStatus).equals(Status.valueOf(oldStatus))) {
                entity.setStatus(newStatus);
                log.info("Chuyển trạng thái bệnh nhân {} từ {} → {}", entity.getPatientId(), oldStatus, newStatus);
            }
        }

        if (request.getCheckoutTime() != null) {
            entity.setCheckoutTime(request.getCheckoutTime());
            log.info("Cập nhật checkoutTime bệnh nhân {} thành {}", entity.getPatientId(), request.getCheckoutTime());
        }

        if (request.getDepartmentId() != null) {
            entity.setDepartmentId(request.getDepartmentId());
        }

        return queuePatientsMapper.toResponse(queuePatientsRepository.save(entity));
    }

    @Override
    public void deleteQueuePatients(String id) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        entity.setDeletedAt(LocalDateTime.now());
        queuePatientsRepository.save(entity);

        log.info("Đã soft delete bệnh nhân {}", entity.getPatientId());
    }

    @Override
    public QueuePatientsResponse getQueuePatientsById(String id) {
        return queuePatientsMapper.toResponse(queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND)));
    }

    @Override
    public List<QueuePatientsResponse> getAllQueuePatients() {
        return queuePatientsRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueuePatientsResponse> getAllQueuePatientsByStatusAndQueueId(String status, String queueId) {
        return queuePatientsRepository.findAllByStatusAndQueueId(status, queueId)
                .stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getMaxQueueOrderForRoom(String departmentId, String queueId) {
        Long max = queuePatientsRepository.findMaxQueueOrderByRoom(departmentId, queueId);
        return (max != null) ? max : 0L;
    }

    @Override
    public List<QueuePatientsResponse> getTopWaitingUnassigned(String queueId, int limit) {
        return queuePatientsRepository.findTopUnassignedWaiting(queueId, limit).stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueuePatientsResponse> getAssignedPatientsForRoom(String queueId, String departmentId) {
        List<String> statuses = List.of(Status.WAITING.name(), Status.IN_PROGRESS.name());
        return queuePatientsRepository.findAssigned(queueId, departmentId, statuses)
                .stream()
                .map(queuePatientsMapper::toResponse)
                .collect(Collectors.toList());
    }


}
