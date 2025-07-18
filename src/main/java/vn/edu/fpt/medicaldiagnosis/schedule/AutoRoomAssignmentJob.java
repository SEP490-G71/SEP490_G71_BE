package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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

/**
 * Job tự động phân phòng cho bệnh nhân đang chờ khám theo lịch định kỳ.
 *
 * Chức năng chính:
 * - Duyệt qua các tenant đang hoạt động.
 * - Gán tối đa 5 bệnh nhân ưu tiên và 5 bệnh nhân thường vào các phòng phù hợp.
 * - Cập nhật thứ tự hàng đợi (queueOrder) và đẩy vào hàng đợi trong bộ nhớ (in-memory).
 * - Khởi tạo RoomWorker cho mỗi phòng (nếu chưa tồn tại).
 * - Gửi email callback nếu bệnh nhân đã đăng ký nhận thông báo.
 */
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
    private final QueuePollingService queuePollingService;

    private final Map<String, RoomQueueHolder> tenantQueues = new ConcurrentHashMap<>();

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
                        holder.registerDepartmentMetadata(roomNumber, department);
                    }
                    return holder;
                });

                // ========= 1. PHÂN BỆNH NHÂN ƯU TIÊN =========
                List<QueuePatientsResponse> priorityPatients = queuePatientsService.getTopWaitingPriority(queueId, 10);
                for (QueuePatientsResponse patient : priorityPatients) {
                    if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) continue;

                    Integer roomNumber = DataUtil.parseInt(patient.getRoomNumber());
                    if (roomNumber == null) {
                        roomNumber = queueHolder.findLeastBusyRoom(patient.getType(), patient.getSpecialization() != null ? patient.getSpecialization().getId() : null);
                    }

                    if (patient.getQueueOrder() == null) {
                        long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(roomNumber), queueId) + 1;
                        boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), roomNumber, nextOrder);
                        if (!updated) continue;
                        patient = queuePatientsService.getQueuePatientsById(patient.getId());
                    }

                    if (!queueHolder.hasRoom(roomNumber)) {
                        queueHolder.initRoom(roomNumber, tenantCode, queuePatientsService, queueId);

                        DepartmentResponse deptMeta = DepartmentResponse.builder()
                                .type(patient.getType())
                                .specialization(patient.getSpecialization())
                                .build();

                        queueHolder.registerDepartmentMetadata(roomNumber, deptMeta);
                        log.info("Khởi tạo mới phòng {} cho patient {}", roomNumber, patient.getPatientId());
                    }

                    Integer finalRoomNumber = roomNumber;
                    queueHolder.enqueuePatientAndNotifyListeners(roomNumber, patient, () -> {
                        queueHolder.refreshQueue(finalRoomNumber, queuePatientsService);
                        queuePollingService.notifyListeners(queuePatientsService.getAllQueuePatients());
                    });

                    handleCallback(patient.getPatientId(), roomNumber, patient.getQueueOrder());
                }

                // ========= 2. PHÂN BỆNH NHÂN THƯỜNG =========
                List<QueuePatientsResponse> normalPatients = queuePatientsService.getTopWaitingUnassigned(queueId, 10);
                for (QueuePatientsResponse patient : normalPatients) {
                    if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) continue;

                    int targetRoom = queueHolder.findLeastBusyRoom(patient.getType(), patient.getSpecialization() != null ? patient.getSpecialization().getId() : null);
                    long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(targetRoom), queueId) + 1;

                    boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), targetRoom, nextOrder);
                    if (!updated) continue;

                    patient = queuePatientsService.getQueuePatientsById(patient.getId());

                    queueHolder.enqueuePatientAndNotifyListeners(targetRoom, patient, () -> {
                        queueHolder.refreshQueue(targetRoom, queuePatientsService);
                        queuePollingService.notifyListeners(queuePatientsService.getAllQueuePatients());
                    });

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

    @PreDestroy
    public void shutdownAllWorkers() {
        tenantQueues.values().forEach(RoomQueueHolder::stopAllWorkers);
        log.info("Đã dừng tất cả RoomWorkers khi tắt ứng dụng.");
    }

    private void handleCallback(String patientId, int room, long order) {
        if (!callbackRegistry.contains(patientId)) return;
        try {
            PatientResponse patient = patientService.getPatientById(patientId);
            emailService.sendRoomAssignmentMail(
                    patient.getEmail(),
                    patient.getFullName() != null && !patient.getFullName().isBlank()
                            ? patient.getFullName()
                            : patient.getFirstName() + " " + patient.getMiddleName() + " " + patient.getLastName(),
                    room,
                    order
            );
            callbackRegistry.remove(patientId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email phân phòng cho bệnh nhân {}: {}", patientId, e.getMessage(), e);
        }
    }
}
