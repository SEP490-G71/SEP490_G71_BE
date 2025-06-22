package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePollingService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class QueuePollingServiceImpl implements QueuePollingService {

    @Autowired
    @Qualifier("deferredResultExecutor")
    private TaskExecutor deferredResultExecutor;

    private final Set<DeferredResult<List<QueuePatientsResponse>>> listeners = new CopyOnWriteArraySet<>();

    @Override
    public DeferredResult<List<QueuePatientsResponse>> registerListener() {
        DeferredResult<List<QueuePatientsResponse>> result = new DeferredResult<>(30000L); // 30 giây timeout

        deferredResultExecutor.execute(() -> listeners.add(result)); // chạy trong thread pool riêng

        result.onCompletion(() -> listeners.remove(result));
        result.onTimeout(() -> listeners.remove(result));
        result.onError((e) -> listeners.remove(result));

        return result;
    }

    @Override
    public void notifyListeners(List<QueuePatientsResponse> updatedList) {
        for (DeferredResult<List<QueuePatientsResponse>> listener : listeners) {
            listener.setResult(updatedList);
        }
        listeners.clear();
    }

    @Override
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }
}
