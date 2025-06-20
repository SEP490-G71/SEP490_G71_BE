package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomQueueHolder;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRoomAssignmentJob {

    private static final int ROOM_CAPACITY = 5;
    private final TenantService tenantService;
    private final QueuePatientsService queuePatientsService;
    private final DailyQueueService dailyQueueService;
    private final Map<String, RoomQueueHolder> tenantQueues = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 2000)
    public void dispatchAndProcess() {
        tenantService.getAllTenantsActive().forEach(tenant -> {
            String tenantCode = tenant.getCode();
            MDC.put("TENANT", tenantCode);
            try {
                TenantContext.setTenantId(tenantCode);
                RoomQueueHolder holder = tenantQueues.computeIfAbsent(tenantCode, t -> {
                    RoomQueueHolder h = new RoomQueueHolder(ROOM_CAPACITY);
                    h.startWorkers(tenantCode, queuePatientsService);
                    return h;
                });

                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                // Lấy tối đa 5 bệnh nhân chờ phân
                List<QueuePatientsResponse> waiting = queuePatientsService.getTopWaitingUnassigned(queueId, 5);

                for (QueuePatientsResponse patient : waiting) {
                    int targetRoom = holder.findLeastBusyRoom();
                    long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(targetRoom), queueId) + 1;

                    queuePatientsService.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                            .queueOrder(nextOrder)
                            .departmentId(String.valueOf(targetRoom))
                            .build());

                    holder.enqueue(targetRoom, patient);
                    log.info("Phân bệnh nhân {} vào phòng {}, thứ tự {}", patient.getPatientId(), targetRoom, nextOrder);
                }

            } catch (Exception e) {
                log.error("Lỗi xử lý AutoRoomAssignmentJob: {}", e.getMessage(), e);
            } finally {
                TenantContext.clear();
                MDC.remove("TENANT");
            }
        });
    }
}
