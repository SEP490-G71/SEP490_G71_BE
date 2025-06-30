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

@Slf4j
public class RoomWorker implements Runnable {

    private final int roomNumber; // Mã phòng khám đang xử lý
    private final String tenantCode; // Tenant hiện tại (cho hệ thống đa tenant)
    private final Queue<QueuePatientsResponse> queue; // Hàng đợi bệnh nhân của phòng
    private final QueuePatientsService service; // Service để xử lý dữ liệu bệnh nhân

    private volatile boolean running = true; // Cờ chạy để điều khiển luồng dừng lại khi cần

    public RoomWorker(int roomNumber, String tenantCode, Queue<QueuePatientsResponse> queue, QueuePatientsService service) {
        this.roomNumber = roomNumber;
        this.tenantCode = tenantCode;
        this.queue = queue;
        this.service = service;
    }

    // Được gọi khi cần dừng thread này (ví dụ khi hệ thống tắt)
    public void stopWorker() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Thiết lập TenantContext để đảm bảo hoạt động đúng tenant khi chạy đa tenant
                TenantContext.setTenantId(tenantCode);

                synchronized (queue) {
                    // Lấy bệnh nhân đầu hàng đợi (peek không xoá)
                    QueuePatientsResponse patient = queue.peek();
                    if (patient == null) {
                        Thread.sleep(500);
                        continue;
                    }

                    QueuePatientsResponse latest;
                    try {
                        latest = service.getQueuePatientsById(patient.getId());
                    } catch (AppException ex) {
                        log.error("RoomWorker phòng {} lỗi: {}. Xóa khỏi queue", roomNumber, ex.getMessage());
                        queue.poll();
                        continue;
                    }

                    String status = latest.getStatus();

                    // Nếu bệnh nhân đã khám xong → cập nhật checkoutTime và loại khỏi hàng đợi
                    if (Status.DONE.name().equalsIgnoreCase(status)) {
                        if (latest.getCheckoutTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .checkoutTime(now)
                                    .build());

                            log.info("Phòng {} hoàn tất bệnh nhân {} — cập nhật checkoutTime vào {}", roomNumber, patient.getPatientId(), now);
                        }
                        queue.poll();
                        continue;
                    }

                    // Nếu bệnh nhân bị huỷ khám → loại khỏi hàng đợi
                    if (Status.CANCELED.name().equalsIgnoreCase(status)) {
                        queue.poll();
                        log.info("Phòng {} loại khỏi hàng đợi bệnh nhân {} (trạng thái: CANCELED)", roomNumber, patient.getPatientId());
                        continue;
                    }

                    // Nếu bệnh nhân đang được khám (IN_PROGRESS) → cập nhật checkinTime
                    if (Status.IN_PROGRESS.name().equalsIgnoreCase(status)) {
                        if (latest.getCheckinTime() == null) {
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .checkinTime(LocalDateTime.now())
                                    .build());

                            log.info("Cập nhật checkinTime bệnh nhân {} vào {}", patient.getPatientId(), LocalDateTime.now());
                        }
                    }

                    // Nếu bệnh nhân đang chờ (WAITING)
                    if (Status.WAITING.name().equalsIgnoreCase(status)) {

                        // Bệnh nhân mới vào hàng đợi delay 10s
                        LocalDateTime assignedTime = patient.getAssignedTime();
                        if (assignedTime != null && assignedTime.isAfter(LocalDateTime.now().minusSeconds(10))) {
                            continue;
                        }

                        // Nếu chưa từng được gọi khám → set thời điểm gọi lần đầu (calledTime)
                        if (latest.getCalledTime() == null) {
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .calledTime(LocalDateTime.now())
                                    .build());
                            log.info("Phòng {} bắt đầu gọi bệnh nhân {}", roomNumber, patient.getPatientId());
                        }

                        // Nếu đã gọi hơn 2 phút mà bệnh nhân chưa vào → huỷ khám
                        else if (latest.getCalledTime().isBefore(LocalDateTime.now().minusMinutes(2))) {
                            service.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                                    .status(Status.CANCELED.name())
                                    .build());
                            log.warn("Bệnh nhân {} không phản hồi sau 2 phút — huỷ khám", patient.getPatientId());
                        }
                    }
                }

                // Chờ 1 giây trước lần xử lý tiếp theo
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Khôi phục trạng thái ngắt
                log.warn("RoomWorker {} bị dừng do interrupt", roomNumber);
                break;

            } catch (Exception e) {
                // Bắt lỗi bất ngờ trong xử lý
                log.error("RoomWorker phòng {} lỗi: {}", roomNumber, e.getMessage(), e);

            } finally {
                // Luôn xóa TenantContext để tránh ảnh hưởng đến tenant khác
                TenantContext.clear();
            }
        }

        log.info("RoomWorker phòng {} đã dừng", roomNumber);
    }
}
