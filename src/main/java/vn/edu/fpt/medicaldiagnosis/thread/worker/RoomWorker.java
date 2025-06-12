package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

@RequiredArgsConstructor
public class RoomWorker extends Thread {

    private final QueuePatientsService queuePatientsService;
    private static final Logger log = LoggerFactory.getLogger(RoomWorker.class);

    private final int roomId;
    private final String roomName;
    private final Queue<QueuePatientsResponse> waitingQueue;
    private final Object lock;
    private final RoomManager roomManager;
    private final String tenantCode;
    private final Map<String, Set<String>> processingPatientIdsMap;

    public RoomWorker(QueuePatientsService queuePatientsService, int roomId,
                      Queue<QueuePatientsResponse> waitingQueue, Object lock,
                      RoomManager roomManager, String tenantCode,
                      Map<String, Set<String>> processingPatientIdsMap) {
        this.queuePatientsService = queuePatientsService;
        this.roomId = roomId;
        this.roomName = "Phòng " + (roomId + 1);
        this.waitingQueue = waitingQueue;
        this.lock = lock;
        this.roomManager = roomManager;
        this.tenantCode = tenantCode;
        this.processingPatientIdsMap = processingPatientIdsMap;
    }

    @Override
    public void run() {
        while (true) {
            QueuePatientsResponse q;

            synchronized (lock) {
                while (waitingQueue.isEmpty()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                q = waitingQueue.poll();
                if (q == null) continue;

                roomManager.markBusy(roomId, true);
            }

            log.info("{} bắt đầu khám cho bệnh nhân: {}", roomName, q.getPatientId());

            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("{} đang chờ bác sĩ xác nhận DONE cho bệnh nhân: {}", roomName, q.getPatientId());

            while (true) {
                try {
                    TenantContext.setTenantId(tenantCode);
                    QueuePatientsResponse updated = queuePatientsService.getQueuePatientsById(q.getId());

                    if ("DONE".equalsIgnoreCase(updated.getStatus())) {
                        log.info("{} đã hoàn tất xác nhận khám bệnh nhân: {}", roomName, updated.getPatientId());
                        break;
                    }

                    log.info("{} vẫn đang bận với bệnh nhân: {} (status: {})", roomName, updated.getPatientId(), updated.getStatus());
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    TenantContext.clear();
                }
            }

            log.info("{} đã hoàn tất xác nhận khám bệnh nhân: {}", roomName, q.getPatientId());

            Set<String> processingSet = processingPatientIdsMap.get(tenantCode);
            if (processingSet != null) {
                processingSet.remove(q.getId());
            }

            roomManager.markBusy(roomId, false);
        }
    }
}
