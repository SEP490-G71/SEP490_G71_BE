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
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.*;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomQueueHolder;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRoomAssignmentJob {

    private final CallbackRegistry callbackRegistry;
    private final EmailService emailService;

    private final TenantService tenantService;
    private final QueuePatientsService queuePatientsService;
    private final DailyQueueService dailyQueueService;
    private final DepartmentService departmentService;
    private final PatientService patientService;

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
            MDC.put("TENANT", tenantCode); // Gán context log để phân biệt log theo tenant

            try {
                TenantContext.setTenantId(tenantCode);

                // Lấy queue hiện tại trong ngày
                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                // Khởi tạo RoomQueueHolder (nếu chưa có) và khởi động RoomWorker cho từng phòng
                RoomQueueHolder queueHolder = tenantQueues.computeIfAbsent(tenantCode, t -> {
                    RoomQueueHolder holder = new RoomQueueHolder();

                    List<DepartmentResponse> departments = departmentService.getAllDepartments();
                    log.info("Tenant {} có {} phòng khám", t, departments.size());

                    for (DepartmentResponse department : departments) {
                        Integer roomNumber = DataUtil.parseInt(department.getRoomNumber());
                        if (roomNumber == null) continue;

                        // Tạo queue + thread xử lý cho mỗi phòng khám
                        holder.initRoom(roomNumber, t, queuePatientsService, queueId);

                        // Gán loại phòng khám tương ứng
                        holder.getRoomTypes().put(roomNumber, department.getType());
                    }

                    return holder;
                });

                // Lấy tối đa 5 bệnh nhân chưa được phân phòng (không phải ưu tiên)
                List<QueuePatientsResponse> waitingList = queuePatientsService.getTopWaitingUnassigned(queueId, 5);

                for (QueuePatientsResponse patient : waitingList) {
                    QueuePatientsResponse latest = queuePatientsService.getQueuePatientsById(patient.getId());

                    // Chỉ xử lý bệnh nhân vẫn đang WAITING
                    if (!Status.WAITING.name().equalsIgnoreCase(latest.getStatus())) {
                        continue;
                    }

                    // Nếu đã có phòng hoặc queueOrder thì bỏ qua (bệnh nhân ưu tiên hoặc khám online)
                    if (latest.getRoomNumber() != null || latest.getQueueOrder() != null) {
                        continue;
                    }

                    // Tìm phòng phù hợp với loại dịch vụ, ít người nhất
                    int targetRoom = queueHolder.findLeastBusyRoom(latest.getType());

                    // Tính toán số thứ tự kế tiếp trong phòng
                    long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(targetRoom), queueId) + 1;

                    // Gán bệnh nhân vào phòng và cập nhật DB nếu chưa có người khác gán trước
                    boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), targetRoom, nextOrder);
                    if (!updated) {
                        continue; // Nếu bị thread khác gán rồi thì bỏ qua
                    }

                    // Đưa bệnh nhân vào queue trong bộ nhớ của phòng đó
                    queueHolder.enqueue(targetRoom, latest);

                    log.info("Phân bệnh nhân {} vào phòng {}, thứ tự {}", patient.getPatientId(), targetRoom, nextOrder);

                    // Nếu bệnh nhân đăng ký callback (ví dụ: gửi email), thì xử lý
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
     * Gửi email nếu bệnh nhân đã được đăng ký callback
     */
    private void handleCallback(String patientId, int room, long order) {
        if (!callbackRegistry.contains(patientId)) return;

        try {
            // Lấy thông tin bệnh nhân
            PatientResponse patient = patientService.getPatientById(patientId);

            emailService.sendRoomAssignmentMail(
                    patient.getEmail(),
                    patient.getFullName() != null && !patient.getFullName().isBlank()
                            ? patient.getFullName()
                            : patient.getFirstName() + " " + patient.getMiddleName() + " " + patient.getLastName(),
                    room,
                    order
            );

            // Xoá khỏi registry sau khi gửi
            callbackRegistry.remove(patientId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email phân phòng cho bệnh nhân {}: {}", patientId, e.getMessage(), e);
        }
    }

}
