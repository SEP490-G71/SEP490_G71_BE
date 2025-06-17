package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyQueueResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.DailyQueueMapper;
import vn.edu.fpt.medicaldiagnosis.repository.DailyQueueRepository;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyQueueServiceImpl implements DailyQueueService {

    private final DailyQueueRepository repository;
    private final DailyQueueMapper mapper;

    @Override
    public DailyQueueResponse createDailyQueue(DailyQueueRequest request) {
        DailyQueue queue = mapper.toEntity(request);
        return mapper.toResponse(repository.save(queue));
    }

    @Override
    public DailyQueueResponse updateDailyQueue(String id, DailyQueueRequest request) {
        DailyQueue queue = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_NOT_FOUND));
        mapper.update(queue, request);
        return mapper.toResponse(repository.save(queue));
    }

    @Override
    public void deleteDailyQueue(String id) {
        DailyQueue queue = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_NOT_FOUND));
        queue.setDeletedAt(LocalDateTime.now());
        repository.save(queue);
    }

    @Override
    public DailyQueueResponse getDailyQueueById(String id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_NOT_FOUND));
    }

    @Override
    public List<DailyQueueResponse> getAllDailyQueues() {
        return repository.findAllByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String getActiveQueueIdForToday() {
        return repository.findFirstByStatusOrderByQueueDateDesc("ACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_NOT_FOUND))
                .getId();
    }

    @Override
    public void closeTodayQueue() {
        repository.findActiveQueueForToday()
                .ifPresent(queue -> {
                    queue.setStatus("INACTIVE");
                    repository.save(queue);
                    log.info("Đã đóng hàng đợi ngày {}", queue.getQueueDate().toLocalDate());
                });
    }

}
