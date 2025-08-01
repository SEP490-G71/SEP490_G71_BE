package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientCompactResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QueuePatientsService {
    QueuePatientsResponse createQueuePatients(QueuePatientsRequest request);
    QueuePatientsResponse updateQueuePatients(String id, QueuePatientsRequest request);
    void deleteQueuePatients(String id);
    QueuePatientsResponse getQueuePatientsById(String id);
    List<QueuePatientsResponse> getAllQueuePatients();

    List<QueuePatientsResponse> getAllQueuePatientsByStatusAndQueueId(String status, String queueId);

    Long getMaxQueueOrderForRoom(String departmentId, String queueId);

    List<QueuePatientsResponse> getTopWaitingUnassigned(String queueId, int limit);

    List<QueuePatientsResponse> getAssignedPatientsForRoom(String queueId, String departmentId);

    boolean tryAssignPatientToRoom(String patientId, int roomId, long queueOrder);

    List<QueuePatientsResponse> getTopWaitingPriority(String queueId, int limit);

    Page<QueuePatientCompactResponse> searchQueuePatients(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    QueuePatientCompactResponse getQueuePatientDetail(String id);

    QueuePatientsResponse updateQueuePatientStatus(String id, String newStatus);
}
