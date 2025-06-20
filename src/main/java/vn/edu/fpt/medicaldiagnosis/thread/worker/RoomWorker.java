package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;

import java.util.Queue;

@Slf4j
public class RoomWorker implements Runnable {

    private final int roomId;
    private final String tenantCode;
    private final Queue<QueuePatientsResponse> queue;
    private final QueuePatientsService service;

    public RoomWorker(int roomId, String tenantCode, Queue<QueuePatientsResponse> queue, QueuePatientsService service) {
        this.roomId = roomId;
        this.tenantCode = tenantCode;
        this.queue = queue;
        this.service = service;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TenantContext.setTenantId(tenantCode);

                synchronized (queue) {
                    QueuePatientsResponse patient = queue.peek();
                    if (patient == null) continue;

                    String status = service.getQueuePatientsById(patient.getId()).getStatus();

                    if (Status.DONE.name().equalsIgnoreCase(status)) {
                        queue.poll(); // remove DONE
                        log.info("Phòng {} đã xong bệnh nhân {}", roomId, patient.getPatientId());
                        continue;
                    }

                    if (Status.WAITING.name().equalsIgnoreCase(status)) {
                        service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                .status(Status.IN_PROGRESS.name())
                                .build());
                        log.info("Phòng {} bắt đầu khám bệnh nhân {}", roomId, patient.getPatientId());
                    }
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("RoomWorker phòng {} lỗi: {}", roomId, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
