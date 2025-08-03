package vn.edu.fpt.medicaldiagnosis.thread.worker;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.TextToSpeechService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
    private final QueuePatientsService queuePatientsService; // Service xử lý dữ liệu hàng đợi bệnh nhân
    private final TextToSpeechService textToSpeechService;
    private final PatientRepository patientRepository;

    private volatile boolean running = true; // Cờ điều khiển để dừng thread khi cần

    // Ghi nhớ thời điểm phát lời gọi gần nhất cho từng bệnh nhân (key: patientId, value: timestamp ms)
    private final Map<String, Long> lastSpeechTimestamps = new HashMap<>();

    // Khoảng thời gian tối thiểu giữa 2 lần gọi audio cho cùng một bệnh nhân (10 giây)
    private static final long SPEECH_INTERVAL_MS = 20_000;


    public RoomWorker(int roomNumber, String tenantCode, Queue<QueuePatientsResponse> queue, QueuePatientsService queuePatientsService, TextToSpeechService textToSpeechService, PatientRepository patientRepository) {
        this.roomNumber = roomNumber;
        this.tenantCode = tenantCode;
        this.queue = queue;
        this.queuePatientsService = queuePatientsService;
        this.textToSpeechService = textToSpeechService;
        this.patientRepository = patientRepository;
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
                    QueuePatientsResponse queuePatientsResponse = queue.peek(); // Lấy bệnh nhân đầu tiên trong hàng đợi

                    if (queuePatientsResponse == null) {
                        Thread.sleep(500);
                        continue;
                    }

                    Patient patient = patientRepository.findByIdAndDeletedAtIsNull(queuePatientsResponse.getPatientId()).orElse(null);

                    // Luôn lấy bản ghi mới nhất từ DB để đảm bảo dữ liệu không bị stale
                    QueuePatientsResponse latest;
                    try {
                        latest = queuePatientsService.getQueuePatientsById(queuePatientsResponse.getId());
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
                            queuePatientsService.updateQueuePatients(queuePatientsResponse.getId(), QueuePatientsRequest.builder()
                                    .checkoutTime(now)
                                    .build());

                            log.info("Phòng {} hoàn tất bệnh nhân {} — cập nhật checkoutTime {}", roomNumber, queuePatientsResponse.getPatientId(), now);
                        }
                        queue.poll();
                        continue;
                    }

                    // 2. Nếu bệnh nhân đã huỷ → loại khỏi hàng đợi
                    if (Status.CANCELED.name().equalsIgnoreCase(status)) {
                        queue.poll();
                        log.info("Phòng {} loại khỏi hàng đợi bệnh nhân {} (trạng thái: CANCELED)", roomNumber, queuePatientsResponse.getPatientId());
                        continue;
                    }

                    // 3. Nếu bệnh nhân đang được khám (IN_PROGRESS) → ghi nhận thời điểm checkin (nếu chưa có)
                    if (Status.IN_PROGRESS.name().equalsIgnoreCase(status)) {
                        if (latest.getCheckinTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            queuePatientsService.updateQueuePatients(queuePatientsResponse.getId(), QueuePatientsRequest.builder()
                                    .checkinTime(now)
                                    .build());
                            log.info("Cập nhật checkinTime bệnh nhân {} vào {}", queuePatientsResponse.getPatientId(), now);
                        }
                    }

                    // 4. Bệnh nhân đang chờ kết quả (AWAITING_RESULT) → cập nhật awaitingResultTime (nếu chưa có)
                    if (Status.AWAITING_RESULT.name().equalsIgnoreCase(status)) {
                        if (latest.getAwaitingResultTime() == null) {
                            LocalDateTime now = LocalDateTime.now();
                            queuePatientsService.updateQueuePatients(queuePatientsResponse.getId(), QueuePatientsRequest.builder()
                                    .awaitingResultTime(now)
                                    .build());
                            log.info("Cập nhật awaitingResultTime bệnh nhân {} vào {}", queuePatientsResponse.getPatientId(), now);
                        }
                    }

                    // 5. Nếu bệnh nhân đang đươc gọi (CALLING)
                    if (Status.CALLING.name().equalsIgnoreCase(status)) {
                        if (latest.getCalledTime() == null) {
                            // Nếu chưa có thời điểm gọi, cập nhật thời gian hiện tại
                            LocalDateTime now = LocalDateTime.now();
                            queuePatientsService.updateQueuePatients(queuePatientsResponse.getId(), QueuePatientsRequest.builder()
                                    .calledTime(now)
                                    .build());

                            log.info("Bệnh nhân {} đang đc gọi vào lúc {}", queuePatientsResponse.getPatientId(), now);
                        }

                        // Lấy thời điểm hiện tại (đơn vị: millisecond)
                        long nowMillis = System.currentTimeMillis();

                        // Lấy thời điểm gần nhất đã phát lời gọi cho bệnh nhân này, mặc định là 0 nếu chưa từng gọi
                        long lastSpoken = lastSpeechTimestamps.getOrDefault(latest.getId(), 0L);

                        // Nếu đã đủ 10 giây kể từ lần phát trước → gọi lại
                        if (nowMillis - lastSpoken >= SPEECH_INTERVAL_MS) {
                            String message = String.format("Mời bệnh nhân %s vào phòng số %d",
                                    patient != null && patient.getFullName() != null ? patient.getFullName() : "Không rõ tên",
                                    roomNumber);

                            // Gửi nội dung đến TextToSpeech để phát qua loa
                            textToSpeechService.speak(message);

                            // Ghi nhận thời điểm phát gần nhất để tránh lặp lại quá sớm
                            lastSpeechTimestamps.put(latest.getId(), nowMillis);
                        }
                    }
                }

                // Tạm nghỉ 1 giây trước khi tiếp tục xử lý lặp kế tiếp
                synchronized (queue) {
                    queue.wait(1000);
                }

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
