package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;

import java.util.List;

public interface QueuePatientsService {
    QueuePatientsResponse createQueuePatients(QueuePatientsRequest request);
    QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request);
    void deleteQueuePatients(String id);
    QueuePatientsResponse getQueuePatientsById(String id);
    List<QueuePatientsResponse> getAllQueuePatients();

    List<QueuePatientsResponse> getAllQueuePatientsByStatus(String status);
}
