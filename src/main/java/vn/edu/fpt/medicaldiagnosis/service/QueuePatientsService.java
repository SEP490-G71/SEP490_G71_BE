package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;

import java.util.List;
import java.util.Optional;

public interface QueuePatientsService {
    QueuePatientsResponse createQueuePatients(QueuePatientsRequest request);
    QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request);
    void deleteQueuePatients(String id);
    QueuePatientsResponse getQueuePatientsById(String id);
    List<QueuePatientsResponse> getAllQueuePatients();

    List<QueuePatientsResponse> getAllQueuePatientsByStatusAndQueueId(String status, String queueId);

    Long getMaxQueueOrderForRoom(String departmentId, String queueId);

    List<QueuePatientsResponse> getTopWaitingUnassigned(String queueId, int i);

    List<QueuePatientsResponse> getAssignedPatientsForRoom(String queueId, String departmentId);

}
