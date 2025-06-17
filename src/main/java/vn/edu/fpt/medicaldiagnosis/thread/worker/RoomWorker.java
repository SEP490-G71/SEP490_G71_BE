package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
 * RoomWorker là một thread đại diện cho một phòng khám.
 * Mỗi RoomWorker lấy bệnh nhân từ hàng đợi, chuyển sang IN_PROGRESS, theo dõi đến khi DONE.
 */
@RequiredArgsConstructor
public class RoomWorker extends Thread {

    private final QueuePatientsService queuePatientsService; // Service xử lý bệnh nhân
    private static final Logger log = LoggerFactory.getLogger(RoomWorker.class);

    private final int roomId;                                 // ID nội bộ của phòng (0-based)
    private final String roomName;                            // Tên hiển thị (vd: "Phòng 1")
    private final Queue<QueuePatientsResponse> waitingQueue;  // Hàng đợi bệnh nhân cần khám
    private final Object lock;                                // Lock dùng để đồng bộ thread
    private final RoomManager roomManager;                    // Quản lý trạng thái phòng khám
    private final String tenantCode;                          // Mã định danh tenant (multi-tenant)
    private final Map<String, Set<String>> processingPatientIdsMap; // Danh sách bệnh nhân đang xử lý theo tenant

    /**
     * Constructor chính (tạo tên phòng tự động)
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

    /**
     * Vòng đời của một phòng khám: lấy bệnh nhân → xử lý → chờ xác nhận DONE
     */
    @Override
    public void run() {
        MDC.put("tenant", tenantCode);
        try {
            while (true) {
                QueuePatientsResponse patient = fetchNextPatient();
                if (patient == null) continue;

                if (!prepareForExamination(patient)) {
                    cleanupAfterSkip(patient);
                    continue;
                }

                simulateExamination();

                monitorUntilDone(patient);

                cleanupAfterDone(patient);
            }
        } finally {
            MDC.remove("tenant");
        }
    }


    /**
     * Lấy bệnh nhân tiếp theo từ hàng đợi (có đồng bộ)
     */
    private QueuePatientsResponse fetchNextPatient() {
        synchronized (lock) {
            while (waitingQueue.isEmpty()) {
                try {
                    lock.wait(); // chờ đến khi có bệnh nhân mới
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            QueuePatientsResponse q = waitingQueue.poll();
            if (q != null) {
                roomManager.markBusy(roomId, true); // đánh dấu phòng bận
            }
            return q;
        }
    }

    /**
     * Chuẩn bị bệnh nhân cho quá trình khám: chuyển WAITING → IN_PROGRESS
     */
    private boolean prepareForExamination(QueuePatientsResponse patient) {
        boolean shouldProceed = false;
        try {
            TenantContext.setTenantId(tenantCode);

            QueuePatientsResponse current = queuePatientsService.getQueuePatientsById(patient.getId());

            if (Status.WAITING.name().equalsIgnoreCase(current.getStatus())) {
                queuePatientsService.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                        .status(Status.IN_PROGRESS.name())
                        .build());
                log.info("{} chuyển trạng thái bệnh nhân {} từ WAITING → IN_PROGRESS", roomName, patient.getPatientId());
                shouldProceed = true;
            } else if (Status.IN_PROGRESS.name().equalsIgnoreCase(current.getStatus())) {
                log.info("{} tiếp tục xử lý bệnh nhân {} đã ở trạng thái IN_PROGRESS", roomName, patient.getPatientId());
                shouldProceed = true;
            } else {
                log.warn("{} bỏ qua bệnh nhân {} vì trạng thái hiện tại là {}", roomName, patient.getPatientId(), current.getStatus());
            }
        } catch (Exception e) {
            log.error("{} lỗi khi kiểm tra/cập nhật trạng thái cho bệnh nhân {}: {}", roomName, patient.getPatientId(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
        return shouldProceed;
    }

    /**
     * Mô phỏng bác sĩ khám trong 5 giây
     */
    private void simulateExamination() {
        try {
            Thread.sleep(5000L); // giả lập thời gian khám
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Theo dõi đến khi bệnh nhân được xác nhận DONE bởi bác sĩ
     */
    private void monitorUntilDone(QueuePatientsResponse patient) {
        log.info("{} đang chờ bác sĩ xác nhận DONE cho bệnh nhân: {}", roomName, patient.getPatientId());

        while (true) {
            try {
                TenantContext.setTenantId(tenantCode);
                QueuePatientsResponse updated = queuePatientsService.getQueuePatientsById(patient.getId());

                if (Status.DONE.name().equalsIgnoreCase(updated.getStatus())) {
                    log.info("{} đã hoàn tất xác nhận khám bệnh nhân: {}", roomName, updated.getPatientId());
                    break;
                }

                // Nếu trạng thái khác IN_PROGRESS thì kết thúc bất thường
                if (!Status.IN_PROGRESS.name().equalsIgnoreCase(updated.getStatus())) {
                    log.warn("{} phát hiện trạng thái bất thường của bệnh nhân {}: {}, kết thúc theo dõi",
                            roomName, updated.getPatientId(), updated.getStatus());
                    break;
                }

                log.info("{} vẫn đang chờ xác nhận DONE cho bệnh nhân: {} (status: {})",
                        roomName, updated.getPatientId(), updated.getStatus());

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Lỗi khi kiểm tra trạng thái bệnh nhân {}: {}", patient.getPatientId(), e.getMessage());
                break;
            } finally {
                TenantContext.clear();
            }
        }
    }

    /**
     * Dọn dẹp nếu bỏ qua bệnh nhân (sai trạng thái)
     */
    private void cleanupAfterSkip(QueuePatientsResponse patient) {
        roomManager.markBusy(roomId, false);
        Set<String> processingSet = processingPatientIdsMap.get(tenantCode);
        if (processingSet != null) {
            processingSet.remove(patient.getId());
        }
    }

    /**
     * Dọn dẹp sau khi hoàn thành bệnh nhân
     */
    private void cleanupAfterDone(QueuePatientsResponse patient) {
        Set<String> processingSet = processingPatientIdsMap.get(tenantCode);
        if (processingSet != null) {
            processingSet.remove(patient.getId());
        }
        roomManager.markBusy(roomId, false);
    }
}
