package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
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

    private final QueuePatientsRepository repository;
    private final QueuePatientsMapper mapper;

    @Override
    public QueuePatientsResponse create(QueuePatientsRequest request) {
        QueuePatients entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public QueuePatientsResponse update(String id, QueuePatientsRequest request) {
        QueuePatients entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));
        mapper.update(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        QueuePatients entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }


    @Override
    public QueuePatientsResponse getById(String id) {
        return mapper.toResponse(repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND)));
    }

    @Override
    public List<QueuePatientsResponse> getAll() {
        return repository.findAllByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
