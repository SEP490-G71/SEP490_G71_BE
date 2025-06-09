package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;

import java.util.List;

public interface QueuePatientsService {
    QueuePatientsResponse create(QueuePatientsRequest request);
    QueuePatientsResponse update(String id, QueuePatientsRequest request);
    void delete(String id);
    QueuePatientsResponse getById(String id);
    List<QueuePatientsResponse> getAll();
}
