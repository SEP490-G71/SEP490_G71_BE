package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.web.context.request.async.DeferredResult;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;

import java.util.List;

public interface QueuePollingService {
    DeferredResult<List<QueuePatientsResponse>> registerListener();
    void notifyListeners(List<QueuePatientsResponse> updatedList);
    boolean hasListeners();
}
