package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.QueuePatientsRepository;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueuePatientsServiceImpl implements QueuePatientsService {

    private final QueuePatientsRepository queuePatientsRepository;
    private final QueuePatientsMapper mapper;

    @Override
    public QueuePatientsResponse createQueuePatients(QueuePatientsRequest request) {
        QueuePatients queue = QueuePatients.builder()
                .queueId(request.getQueueId())
                .patientId(request.getPatientId())
                .queueOrder(request.getQueueOrder())
                .status(request.getStatus() != null ? request.getStatus() : Status.WAITING.name())
                .checkinTime(request.getCheckinTime() != null ? request.getCheckinTime() : LocalDateTime.now())
                .checkoutTime(request.getCheckoutTime())
                .build();

        return mapper.toResponse(queuePatientsRepository.save(queue));
    }



    @Override
    public QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));

        mapper.update(entity, request);
        return mapper.toResponse(queuePatientsRepository.save(entity));
    }


    @Override
    public void deleteQueuePatients(String id) {
        QueuePatients entity = queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        queuePatientsRepository.save(entity);
    }


    @Override
    public QueuePatientsResponse getQueuePatientsById(String id) {
        return mapper.toResponse(queuePatientsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND)));
    }

    @Override
    public List<QueuePatientsResponse> getAllQueuePatients() {
        return queuePatientsRepository.findAllByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueuePatientsResponse> getAllQueuePatientsByStatus(String status) {
        return queuePatientsRepository.findAllByStatusAndDeletedAtIsNull(status)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

}
