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

            try {
                TenantContext.setTenantId(tenantCode);

                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                RoomQueueHolder queueHolder = tenantQueues.computeIfAbsent(tenantCode, t -> {
                    RoomQueueHolder holder = new RoomQueueHolder();
                    List<DepartmentResponse> departments = departmentService.getAllDepartments();
                    log.info("Tenant {} có {} phòng khám", t, departments.size());

                    for (DepartmentResponse department : departments) {
                        Integer roomNumber = DataUtil.parseInt(department.getRoomNumber());
                        if (roomNumber == null) continue;
                        holder.initRoom(roomNumber, t, queuePatientsService, queueId);
                        holder.getRoomTypes().put(roomNumber, department.getType());
                    }

                    return holder;
                });

                // 1. Ưu tiên: chỉ định sẵn phòng (roomNumber != null)
                List<QueuePatientsResponse> priorityPatients = queuePatientsService.getTopWaitingPriority(queueId, 10);
                for (QueuePatientsResponse patient : priorityPatients) {
                    if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) continue;

                    Integer roomNumber = DataUtil.parseInt(patient.getRoomNumber());
                    if (roomNumber == null) continue;

                    // Gán queueOrder nếu chưa có
                    if (patient.getQueueOrder() == null) {
                        long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(roomNumber), queueId) + 1;
                        boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), roomNumber, nextOrder);
                        if (!updated) continue;

                        // Cập nhật lại để enqueue đúng thông tin
                        patient = queuePatientsService.getQueuePatientsById(patient.getId());
                    }

                    if (!queueHolder.hasRoom(roomNumber)) {
                        queueHolder.initRoom(roomNumber, tenantCode, queuePatientsService, queueId);
                        queueHolder.getRoomTypes().put(roomNumber, patient.getType());
                        log.info("Khởi tạo mới phòng {} cho patient {}", roomNumber, patient.getPatientId());
                    }

                    queueHolder.enqueue(roomNumber, patient);
                    handleCallback(patient.getPatientId(), roomNumber, patient.getQueueOrder());
                }

                // 2. Bệnh nhân thường: chưa có phòng & chưa có thứ tự
                List<QueuePatientsResponse> normalPatients = queuePatientsService.getTopWaitingUnassigned(queueId, 10);
                for (QueuePatientsResponse patient : normalPatients) {
                    if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) continue;

                    int targetRoom = queueHolder.findLeastBusyRoom(patient.getType());
                    long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(targetRoom), queueId) + 1;

                    boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), targetRoom, nextOrder);
                    if (!updated) continue;

                    patient = queuePatientsService.getQueuePatientsById(patient.getId());
                    queueHolder.enqueue(targetRoom, patient);
                    log.info("Phân bệnh nhân {} vào phòng {}, thứ tự {}", patient.getPatientId(), targetRoom, nextOrder);

                    handleCallback(patient.getPatientId(), targetRoom, nextOrder);
                }

            } catch (Exception e) {
                log.error("Lỗi xử lý AutoRoomAssignmentJob cho tenant {}: {}", tenantCode, e.getMessage(), e);
            } finally {
                TenantContext.clear();
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
