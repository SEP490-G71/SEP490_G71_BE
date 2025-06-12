package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Lớp RoomWorker đại diện cho một phòng khám (mỗi phòng chạy trong 1 thread riêng).
 * Mỗi RoomWorker chịu trách nhiệm xử lý bệnh nhân từ hàng đợi,
 * đánh dấu trạng thái phòng, và theo dõi tiến trình khám đến khi hoàn tất.
 */
@RequiredArgsConstructor
public class RoomWorker extends Thread {

    private final QueuePatientsService queuePatientsService; // Service xử lý QueuePatients
    private static final Logger log = LoggerFactory.getLogger(RoomWorker.class);

    private final int roomId;                                // ID nội bộ của phòng (bắt đầu từ 0)
    private final String roomName;                           // Tên hiển thị của phòng (vd: "Phòng 1")
    private final Queue<QueuePatientsResponse> waitingQueue; // Hàng đợi bệnh nhân đang chờ được khám
    private final Object lock;                               // Đối tượng đồng bộ để wait/notify giữa các thread
    private final RoomManager roomManager;                   // Quản lý trạng thái bận/rảnh của các phòng
    private final String tenantCode;                         // Mã định danh của tenant hiện tại (multi-tenant)
    private final Map<String, Set<String>> processingPatientIdsMap; // Map chứa danh sách bệnh nhân đang được xử lý cho từng tenant

    /**
     * Constructor để khởi tạo luồng khám bệnh
     */
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

            // Chờ đến khi hàng đợi có bệnh nhân
            synchronized (lock) {
                while (waitingQueue.isEmpty()) {
                    try {
                        lock.wait(); // Nếu hàng đợi trống, thread tạm dừng chờ notify
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Lấy bệnh nhân ra khỏi hàng đợi
                q = waitingQueue.poll();
                if (q == null) continue;

                // Đánh dấu phòng đang bận
                roomManager.markBusy(roomId, true);
            }

            log.info("{} bắt đầu khám cho bệnh nhân: {}", roomName, q.getPatientId());

            try {
                // Thiết lập tenant context để gọi đúng DB schema
                TenantContext.setTenantId(tenantCode);

                // Cập nhật trạng thái bệnh nhân sang "IN_PROGRESS"
                QueuePatientsRequest queuePatientsRequest = QueuePatientsRequest.builder()
                        .status(Status.IN_PROGRESS.name())
                        .build();
                queuePatientsService.updateQueuePatients(q.getId(), queuePatientsRequest);
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật trạng thái PROCESSING cho bệnh nhân {}: {}", q.getPatientId(), e.getMessage());
            } finally {
                TenantContext.clear(); // Clear context để tránh ảnh hưởng các thread khác
            }

            try {
                // Mô phỏng thời gian bác sĩ khám bệnh (5 giây)
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("{} đang chờ bác sĩ xác nhận DONE cho bệnh nhân: {}", roomName, q.getPatientId());

            // Lặp kiểm tra đến khi bệnh nhân được xác nhận DONE
            while (true) {
                try {
                    TenantContext.setTenantId(tenantCode);

                    // Lấy lại thông tin bệnh nhân
                    QueuePatientsResponse updated = queuePatientsService.getQueuePatientsById(q.getId());

                    // Nếu bác sĩ đã xác nhận DONE thì thoát vòng lặp
                    if (Status.DONE.name().equalsIgnoreCase(updated.getStatus())) {
                        log.info("{} đã hoàn tất xác nhận khám bệnh nhân: {}", roomName, updated.getPatientId());
                        break;
                    }

                    // Nếu chưa DONE thì tiếp tục chờ 5 giây rồi kiểm tra lại
                    log.info("{} vẫn đang chờ xác nhận DONE cho bệnh nhân: {} (status: {})", roomName, updated.getPatientId(), updated.getStatus());
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Lỗi khi kiểm tra trạng thái bệnh nhân {}: {}", q.getPatientId(), e.getMessage());
                    break;
                } finally {
                    TenantContext.clear(); // Clear sau mỗi lần gọi API
                }
            }

            // Sau khi bệnh nhân được khám xong → xóa khỏi danh sách đang xử lý
            Set<String> processingSet = processingPatientIdsMap.get(tenantCode);
            if (processingSet != null) {
                processingSet.remove(q.getId());
            }

            // Đánh dấu phòng đã rảnh
            roomManager.markBusy(roomId, false);
        }
    }

}
