package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyQueueResponse;

import java.util.List;

public interface DailyQueueService {
    DailyQueueResponse createDailyQueue(DailyQueueRequest request);
    DailyQueueResponse updateDailyQueue(String id, DailyQueueRequest request);
    void deleteDailyQueue(String id);
    DailyQueueResponse getDailyQueueById(String id);
    List<DailyQueueResponse> getAllDailyQueues();
}
