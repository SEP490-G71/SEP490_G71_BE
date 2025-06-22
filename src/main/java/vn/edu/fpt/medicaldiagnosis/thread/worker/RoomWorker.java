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

    // Dùng để tắt worker khi shutdown
    private volatile boolean running = true;

    public RoomWorker(int roomId, String tenantCode, Queue<QueuePatientsResponse> queue, QueuePatientsService service) {
        this.roomId = roomId;
        this.tenantCode = tenantCode;
        this.queue = queue;
        this.service = service;
    }

    public void stopWorker() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                TenantContext.setTenantId(tenantCode);

                synchronized (queue) {
                    QueuePatientsResponse patient = queue.peek();
                    if (patient == null) {
                        // Không có bệnh nhân, chờ 500ms rồi thử lại
                        Thread.sleep(500);
                        continue;
                    }

                    String status = service.getQueuePatientsById(patient.getId()).getStatus();

                    // Nếu bệnh nhân đã xong hoặc bị hủy thì loại khỏi hàng đợi
                    if (Status.DONE.name().equalsIgnoreCase(status) ||
                            Status.CANCELED.name().equalsIgnoreCase(status)) {
                        queue.poll();
                        log.info("Phòng {} loại khỏi hàng đợi bệnh nhân {} (trạng thái: {})", roomId, patient.getPatientId(), status);
                        continue;
                    }

                    // Nếu chưa khám thì cập nhật sang trạng thái đang khám
                    if (Status.WAITING.name().equalsIgnoreCase(status)) {
                        service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                .status(Status.IN_PROGRESS.name())
                                .build());
                        log.info("Phòng {} bắt đầu khám bệnh nhân {}", roomId, patient.getPatientId());
                    }
                }

                // Chờ 1 giây trước lần xử lý tiếp theo
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("RoomWorker {} bị dừng do interrupt", roomId);
                break;

            } catch (Exception e) {
                log.error("RoomWorker phòng {} lỗi: {}", roomId, e.getMessage(), e);

            } finally {
                TenantContext.clear();
            }
        }

        log.info("RoomWorker phòng {} đã dừng", roomId);
    }
}
