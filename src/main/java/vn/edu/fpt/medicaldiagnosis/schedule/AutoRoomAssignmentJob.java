package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomQueueHolder;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRoomAssignmentJob {

    private static final int ROOM_CAPACITY = 5;

    private final TenantService tenantService;
    private final QueuePatientsService queuePatientsService;
    private final DailyQueueService dailyQueueService;
    private final CallbackRegistry callbackRegistry;
    private final RestTemplate restTemplate;

    private final Map<String, RoomQueueHolder> tenantQueues = new ConcurrentHashMap<>();

    /**
     * Lập lịch thực hiện mỗi 2 giây:
     * - Duyệt tất cả tenant đang hoạt động
     * - Với mỗi tenant, gán tối đa 5 bệnh nhân chưa có phòng vào phòng ít người nhất
     * - Cập nhật queue_order vào DB và đưa vào in-memory queue tương ứng
     */
    @Scheduled(fixedDelay = 2000)
    public void dispatchAndProcess() {
        tenantService.getAllTenantsActive().parallelStream().forEach(tenant -> {
            String tenantCode = tenant.getCode();
            MDC.put("TENANT", tenantCode);
            try {
                TenantContext.setTenantId(tenantCode);

                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                // Khởi tạo worker và queue nếu chưa có
                RoomQueueHolder queueHolder = tenantQueues.computeIfAbsent(tenantCode, t -> {
                    RoomQueueHolder holder = new RoomQueueHolder(ROOM_CAPACITY);
                    holder.restore(queueId, queuePatientsService);
                    holder.startWorkers(t, queuePatientsService);
                    return holder;
                });

                // Lấy tối đa 5 bệnh nhân chưa có phòng
                List<QueuePatientsResponse> waitingList = queuePatientsService.getTopWaitingUnassigned(queueId, 5);

                for (QueuePatientsResponse patient : waitingList) {
                    int targetRoom = queueHolder.findLeastBusyRoom();
                    long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(targetRoom), queueId) + 1;

                    // Cập nhật thông tin vào DB
                    queuePatientsService.updateQueuePatients(patient.getId(), QueuePatientsRequest.builder()
                            .queueOrder(nextOrder)
                            .departmentId(String.valueOf(targetRoom))
                            .build());

                    // Đưa vào in-memory queue phòng
                    queueHolder.enqueue(targetRoom, patient);
                    log.info("Phân bệnh nhân {} vào phòng {}, thứ tự {}", patient.getPatientId(), targetRoom, nextOrder);

                    // Gửi callback nếu có
                    handleCallback(patient.getPatientId(), targetRoom, nextOrder);
                }

            } catch (Exception e) {
                log.error("Lỗi xử lý AutoRoomAssignmentJob cho tenant {}: {}", tenantCode, e.getMessage(), e);
            } finally {
                TenantContext.clear();
                MDC.remove("TENANT");
            }
        });
    }

    /**
     * Dừng toàn bộ worker khi hệ thống shutdown
     */
    @PreDestroy
    public void shutdownAllWorkers() {
        tenantQueues.values().forEach(RoomQueueHolder::stopAllWorkers);
        log.info("Đã dừng tất cả RoomWorkers khi tắt ứng dụng.");
    }

    /**
     * Gửi callback đến URL đã đăng ký nếu có
     */
    private void handleCallback(String patientId, int room, long order) {
        callbackRegistry.get(patientId).ifPresent(callbackUrl -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = Map.of(
                        "patientId", patientId,
                        "room", room,
                        "order", order
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, entity, String.class);

                log.info("Gửi callback thành công đến {}", callbackUrl);
                log.info("Phản hồi từ callback: {}", response.getBody());

                callbackRegistry.remove(patientId);
            } catch (Exception e) {
                log.error("Gửi callback thất bại đến {}: {}", callbackUrl, e.getMessage());
            }
        });
    }
}
