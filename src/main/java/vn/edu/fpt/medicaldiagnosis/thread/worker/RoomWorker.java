package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;

import java.time.LocalDateTime;
import java.util.Queue;

/**
 * Luồng xử lý riêng cho mỗi phòng khám.
 * Mỗi RoomWorker chịu trách nhiệm duyệt danh sách bệnh nhân trong phòng tương ứng,
 * cập nhật trạng thái theo thời gian thực (gọi khám, vào khám, hoàn tất, huỷ...).
 */
@Slf4j
public class RoomWorker implements Runnable {

    private final int roomNumber; // Mã phòng khám đang xử lý
    private final String tenantCode; // Mã tenant hiện tại (phân biệt trong hệ thống đa tenant)
    private final Queue<QueuePatientsResponse> queue; // Hàng đợi bệnh nhân của phòng
    private final QueuePatientsService service; // Service xử lý dữ liệu hàng đợi bệnh nhân

    private volatile boolean running = true; // Cờ điều khiển để dừng thread khi cần

    public RoomWorker(int roomNumber, String tenantCode, Queue<QueuePatientsResponse> queue, QueuePatientsService service) {
        this.roomNumber = roomNumber;
        this.tenantCode = tenantCode;
        this.queue = queue;
        this.service = service;
    }

    /**
     * Gửi tín hiệu dừng cho thread này.
     * Sử dụng trong các tình huống như tắt hệ thống hoặc tenant bị tắt.
     */
    public void stopWorker() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Thiết lập ngữ cảnh tenant để đảm bảo đúng DB khi xử lý
                TenantContext.setTenantId(tenantCode);

                synchronized (queue) {
                    QueuePatientsResponse patient = queue.peek(); // Lấy bệnh nhân đầu tiên trong hàng đợi

                    if (patient == null) {
                        Thread.sleep(500);
                        continue;
                    }

                    // Luôn lấy bản ghi mới nhất từ DB để đảm bảo dữ liệu không bị stale
                    QueuePatientsResponse latest;
                    try {
                        latest = service.getQueuePatientsById(patient.getId());
                    } catch (AppException ex) {
                        log.error("RoomWorker phòng {} lỗi: {}. Xoá bệnh nhân khỏi hàng đợi", roomNumber, ex.getMessage());
                        queue.poll(); // Xoá bản ghi hỏng khỏi queue
                        continue;
                    }

                    String status = latest.getStatus();

                    // 1. Nếu bệnh nhân đã hoàn tất khám → cập nhật checkoutTime nếu chưa có, sau đó xoá khỏi hàng đợi
                    if (Status.DONE.name().equalsIgnoreCase(status)) {
                        if (latest.getCheckoutTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .checkoutTime(now)
                                    .build());

                            log.info("Phòng {} hoàn tất bệnh nhân {} — cập nhật checkoutTime {}", roomNumber, patient.getPatientId(), now);
                        }
                        queue.poll();
                        continue;
                    }

                    // 2. Nếu bệnh nhân đã huỷ → loại khỏi hàng đợi
                    if (Status.CANCELED.name().equalsIgnoreCase(status)) {
                        queue.poll();
                        log.info("Phòng {} loại khỏi hàng đợi bệnh nhân {} (trạng thái: CANCELED)", roomNumber, patient.getPatientId());
                        continue;
                    }

                    // 3. Nếu bệnh nhân đang được khám (IN_PROGRESS) → ghi nhận thời điểm checkin (nếu chưa có)
                    if (Status.IN_PROGRESS.name().equalsIgnoreCase(status)) {
                        if (latest.getCheckinTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .checkinTime(now)
                                    .build());
                            log.info("Cập nhật checkinTime bệnh nhân {} vào {}", patient.getPatientId(), now);
                        }
                    }

                    // 4. Nếu bệnh nhân đang chờ khám (WAITING)
                    if (Status.WAITING.name().equalsIgnoreCase(status)) {

                        // Delay tối thiểu 10s sau khi được gán vào phòng để tránh gọi quá sớm
                        LocalDateTime assignedTime = patient.getAssignedTime();
                        if (assignedTime != null && assignedTime.isAfter(LocalDateTime.now().minusSeconds(10))) {
                            continue;
                        }

                        // 4.1 Nếu chưa từng được gọi khám → cập nhật calledTime
                        if (latest.getCalledTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .calledTime(now)
                                    .build());
                            log.info("Phòng {} bắt đầu gọi bệnh nhân {}", roomNumber, patient.getPatientId());
                        }

                        // 4.2 Nếu đã gọi > 2 phút mà bệnh nhân chưa phản hồi (IN_PROGRESS) → huỷ khám
                        else if (latest.getCalledTime().isBefore(LocalDateTime.now().minusMinutes(2))) {
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .status(Status.CANCELED.name())
                                    .build());
                            log.warn("Bệnh nhân {} không phản hồi sau 2 phút — huỷ khám", patient.getPatientId());
                        }
                    }
                }

                // Tạm nghỉ 1 giây trước khi tiếp tục xử lý lặp kế tiếp
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Đánh dấu trạng thái interrupted
                log.warn("RoomWorker {} bị dừng do interrupt", roomNumber);
                break;

            } catch (Exception e) {
                // Bắt và log mọi lỗi xảy ra trong quá trình xử lý bệnh nhân
                log.error("RoomWorker phòng {} gặp lỗi: {}", roomNumber, e.getMessage(), e);

            } finally {
                // Luôn clear tenant context để không rò rỉ tenant giữa các thread
                TenantContext.clear();
            }
        }

        log.info("RoomWorker phòng {} đã dừng", roomNumber);
    }
}
