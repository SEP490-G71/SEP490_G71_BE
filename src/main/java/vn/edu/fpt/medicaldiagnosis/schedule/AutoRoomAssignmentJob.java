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

    // ================== SERVICE VÀ COMPONENT LIÊN QUAN ==================

    private final CallbackRegistry callbackRegistry;     // Quản lý danh sách bệnh nhân cần gửi email callback
    private final EmailService emailService;             // Gửi email phân phòng cho bệnh nhân
    private final TenantService tenantService;           // Quản lý danh sách tenant đang hoạt động
    private final QueuePatientsService queuePatientsService; // Quản lý thông tin bệnh nhân trong hàng đợi
    private final DailyQueueService dailyQueueService;   // Lấy hàng đợi đang hoạt động theo ngày
    private final DepartmentService departmentService;   // Quản lý thông tin phòng khám
    private final PatientService patientService;         // Truy vấn thông tin chi tiết của bệnh nhân
    private final QueuePollingService queuePollingService; // Gửi thông báo cập nhật WebSocket cho FE

    /**
     * Danh sách RoomQueueHolder theo từng tenant (key = tenantCode).
     * Mỗi holder chứa queue + worker cho từng phòng thuộc tenant đó.
     */
    private final Map<String, RoomQueueHolder> tenantQueues = new ConcurrentHashMap<>();

    // ================== LỊCH CHẠY PHÂN PHÒNG ==================

    /**
     * Job chạy định kỳ mỗi 2 giây.
     * Đối với mỗi tenant:
     * - Lấy danh sách bệnh nhân ưu tiên có phòng chỉ định sẵn và phân vào phòng đó.
     * - Lấy danh sách bệnh nhân thường chưa có phòng → phân vào phòng cùng loại, ít bệnh nhân nhất.
     */
    @Scheduled(fixedDelay = 2000)
    public void dispatchAndProcess() {
        tenantService.getAllTenantsActive().parallelStream().forEach(tenant -> {
            String tenantCode = tenant.getCode();

            try {
                TenantContext.setTenantId(tenantCode);

                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                // Tạo mới RoomQueueHolder nếu tenant chưa có
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

                // ========= 1. PHÂN BỆNH NHÂN ƯU TIÊN =========
                List<QueuePatientsResponse> priorityPatients = queuePatientsService.getTopWaitingPriority(queueId, 10);
                for (QueuePatientsResponse patient : priorityPatients) {
                    if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) continue;

                    Integer roomNumber;
                    if (patient.getRoomNumber() == null) {
                        // Nếu không có room chỉ định → chọn phòng phù hợp như thường
                        roomNumber = queueHolder.findLeastBusyRoom(patient.getType());
                    } else {
                        roomNumber = DataUtil.parseInt(patient.getRoomNumber());
                    }

                    if (roomNumber == null) {
                        log.warn("Không thể xác định phòng cho bệnh nhân {}, bỏ qua", patient.getPatientId());
                        continue;
                    }

                    // Gán queue_order nếu chưa có
                    if (patient.getQueueOrder() == null) {
                        long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(roomNumber), queueId) + 1;
                        boolean updated = queuePatientsService.tryAssignPatientToRoom(patient.getId(), roomNumber, nextOrder);
                        if (!updated) continue;

                        patient = queuePatientsService.getQueuePatientsById(patient.getId());
                    }

                    // Khởi tạo RoomWorker nếu chưa có
                    if (!queueHolder.hasRoom(roomNumber)) {
                        queueHolder.initRoom(roomNumber, tenantCode, queuePatientsService, queueId);
                        queueHolder.getRoomTypes().put(roomNumber, patient.getType());
                        log.info("Khởi tạo mới phòng {} cho patient {}", roomNumber, patient.getPatientId());
                    }

                    // Đưa vào hàng đợi và cập nhật UI/FE
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

                    int targetRoom = queueHolder.findLeastBusyRoom(patient.getType());
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

    // ================== TẮT WORKER ==================

    /**
     * Dừng toàn bộ RoomWorker khi ứng dụng shutdown.
     * Được gọi tự động bởi @PreDestroy.
     */
    @PreDestroy
    public void shutdownAllWorkers() {
        tenantQueues.values().forEach(RoomQueueHolder::stopAllWorkers);
        log.info("Đã dừng tất cả RoomWorkers khi tắt ứng dụng.");
    }

    // ================== CALLBACK GỬI EMAIL ==================

    /**
     * Gửi email thông báo phân phòng cho bệnh nhân nếu họ đã đăng ký callback.
     *
     * @param patientId ID bệnh nhân
     * @param room      Mã phòng khám được phân
     * @param order     Thứ tự trong hàng đợi
     */
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
