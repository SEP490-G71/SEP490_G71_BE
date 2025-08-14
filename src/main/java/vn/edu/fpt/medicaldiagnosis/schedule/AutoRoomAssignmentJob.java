package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
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
    private final TextToSpeechService textToSpeechService;
    private final WorkScheduleService workScheduleService;
    private final SettingService settingService;
    private final PatientRepository patientRepository;

    private final Map<String, RoomQueueHolder> tenantQueues = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 2000)
    public void dispatchAndProcess() {
        tenantService.getAllTenantsActive().parallelStream().forEach(tenant -> {
            String tenantCode = tenant.getCode();
            try {
                TenantContext.setTenantId(tenantCode);
                String queueId = dailyQueueService.getActiveQueueIdForToday();
                if (queueId == null) return;

                RoomQueueHolder queueHolder = tenantQueues.computeIfAbsent(tenantCode, t -> initTenantQueues(t, queueId));

                // ========= 1. PHÂN BỆNH NHÂN ƯU TIÊN =========
                dispatchPatients(queueHolder, tenantCode, queueId, queuePatientsService.getTopWaitingPriority(queueId, 1000));

                // ========= 2. PHÂN BỆNH NHÂN THƯỜNG =========
                dispatchPatients(queueHolder, tenantCode, queueId, queuePatientsService.getTopWaitingUnassigned(queueId, 1000));

                // ========= 3. ĐỒNG BỘ TRẠNG THÁI QUÁ TẢI LÊN DB =========
                reconcileOverloadStatus(queueHolder, queueId);

            } catch (Exception e) {
                log.error("Lỗi xử lý AutoRoomAssignmentJob cho tenant {}: {}", tenantCode, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        });
    }

    private RoomQueueHolder initTenantQueues(String tenantCode, String queueId) {
        RoomQueueHolder holder = new RoomQueueHolder();
        List<DepartmentResponse> departments = departmentService.getAllAvailableDepartments();
        for (DepartmentResponse dep : departments) {
            long countShifts = workScheduleService.countShiftsToday(dep.getId());
            Integer docShiftQuota = settingService.getSetting().getDocShiftQuota();
            Integer roomNumber = DataUtil.parseInt(dep.getRoomNumber());
            if (roomNumber == null) continue;

            holder.initRoom(roomNumber, tenantCode, queuePatientsService, queueId, textToSpeechService, patientRepository);
            holder.registerDepartmentMetadata(roomNumber, dep);

            // ===== Set capacity cho mỗi phòng =====
            int capacity = (docShiftQuota == null) ? 0 : Math.toIntExact(countShifts * (long) docShiftQuota);
            holder.setCapacity(roomNumber, capacity);

            log.info("Tenant {} - Room {} capacity set = {} (countShifts={}, docShiftQuota={})",
                    tenantCode, roomNumber, capacity, countShifts, docShiftQuota);
        }
        log.info("Tenant {}: đã khởi tạo {} phòng khám", tenantCode, departments.size());
        return holder;
    }

    private void dispatchPatients(RoomQueueHolder queueHolder, String tenantCode, String queueId, List<QueuePatientsResponse> patients) {
        for (QueuePatientsResponse patient : patients) {
            if (!Status.WAITING.name().equalsIgnoreCase(patient.getStatus())
                    && !Status.CALLING.name().equalsIgnoreCase(patient.getStatus())) continue;

            Integer roomNumber = (patient.getRoomNumber() != null)
                    ? DataUtil.parseInt(patient.getRoomNumber())
                    : null;

            // Nếu đã có phòng → check quá tải ngay
            if (roomNumber != null && !queueHolder.canAcceptNewPatient(roomNumber)) {
                log.info("Phòng {} đã đầy — bỏ qua patient {}", roomNumber, patient.getPatientId());
                continue;
            }

            // Nếu chưa có phòng → tìm phòng phù hợp
            if (roomNumber == null) {
                roomNumber = queueHolder.findLeastBusyRoom(
                        patient.getType(),
                        patient.getSpecialization() != null ? patient.getSpecialization().getId() : null
                );
                if (roomNumber == null || !queueHolder.canAcceptNewPatient(roomNumber)) {
                    log.info("Không có phòng phù hợp hoặc phòng {} đã đầy — bỏ qua patient {}", roomNumber, patient.getPatientId());
                    continue;
                }
            }

            // Gán queueOrder nếu chưa có
            if (patient.getQueueOrder() == null) {
                long nextOrder = queuePatientsService.getMaxQueueOrderForRoom(String.valueOf(roomNumber), queueId) + 1;
                if (!queuePatientsService.tryAssignPatientToRoom(patient.getId(), roomNumber, nextOrder)) continue;
                patient = queuePatientsService.getQueuePatientsById(patient.getId());
            }

            // Khởi tạo phòng nếu chưa có
            if (!queueHolder.hasRoom(roomNumber)) {
                queueHolder.initRoom(roomNumber, tenantCode, queuePatientsService, queueId, textToSpeechService, patientRepository);
                queueHolder.registerDepartmentMetadata(roomNumber, DepartmentResponse.builder()
                        .type(patient.getType())
                        .specialization(patient.getSpecialization())
                        .build());
                log.info("Khởi tạo mới phòng {} cho patient {}", roomNumber, patient.getPatientId());
            }

            Integer finalRoomNumber = roomNumber;
            queueHolder.enqueuePatientAndNotifyListeners(roomNumber, patient, () -> {
                queueHolder.refreshQueue(finalRoomNumber, queuePatientsService);
                queuePollingService.notifyListeners(queuePatientsService.getAllQueuePatients());
            });

            handleCallback(patient.getPatientId(), roomNumber, patient.getQueueOrder());
        }
    }

    /**
     * Đồng bộ trạng thái quá tải (overloaded) từ DB.
     *
     * Chức năng:
     * - Duyệt qua tất cả phòng trong RoomQueueHolder.
     * - Đếm số bệnh nhân đang active trong DB và so sánh với capacity.
     * - Nếu khác nhau → gọi DepartmentService.updateDepartment() để cập nhật.
     */
    private void reconcileOverloadStatus(RoomQueueHolder queueHolder, String queueId) {
        queueHolder.getCapacities().forEach((roomNumber, capacity) -> {
            try {
                long activeCount = queuePatientsService.countActivePatientsInRoom(String.valueOf(roomNumber), queueId);
                boolean overloadedNow = activeCount >= capacity;

                DepartmentResponse meta = departmentService.getDepartmentByRoomNumber(String.valueOf(roomNumber));
                if (meta == null) return;

                if (meta.isOverloaded() != overloadedNow) {
                    DepartmentUpdateRequest req = DepartmentUpdateRequest.builder()
                            .name(meta.getName())
                            .description(meta.getDescription())
                            .roomNumber(meta.getRoomNumber())
                            .type(meta.getType())
                            .specializationId(meta.getSpecialization() != null ? meta.getSpecialization().getId() : null)
                            .overloaded(overloadedNow)
                            .build();
                    departmentService.updateDepartment(meta.getId(), req);
                    log.info("Đã cập nhật trạng thái quá tải của phòng {} → {}", roomNumber, overloadedNow);
                }
            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ trạng thái quá tải cho phòng {}: {}", roomNumber, e.getMessage(), e);
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
