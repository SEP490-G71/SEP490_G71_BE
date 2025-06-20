package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRoomAssignmentJob {

    private final TenantService tenantService;
    private final QueuePatientsService queuePatientsService;
    private final DailyQueueService dailyQueueService;

    // Số phòng khám mặc định được khởi tạo cho mỗi tenant
    private static final int ROOM_CAPACITY = 5;

    // Các tenant đã được khởi tạo hệ thống phòng khám
    private final Set<String> initializedTenants = ConcurrentHashMap.newKeySet();

    // Hàng đợi bệnh nhân đang chờ xử lý theo từng tenant
    private final Map<String, Queue<QueuePatientsResponse>> tenantQueues = new ConcurrentHashMap<>();

    // Lock dùng để đồng bộ giữa các thread của từng tenant
    private final Map<String, Object> tenantLocks = new ConcurrentHashMap<>();

    // Quản lý trạng thái bận/rảnh của các phòng cho từng tenant
    private final Map<String, RoomManager> tenantRoomManagers = new ConcurrentHashMap<>();

    // Danh sách bệnh nhân đang được xử lý, nhằm tránh xử lý trùng
    private final Map<String, Set<String>> processingPatientIdsMap = new ConcurrentHashMap<>();

    /**
     * Scheduler chạy định kỳ mỗi 10 giây để phân tích và xử lý toàn bộ tenant.
     * Nếu tenant chưa được khởi tạo phòng, sẽ khởi tạo.
     * Nếu đã khởi tạo, chỉ dispatch thêm bệnh nhân mới.
     */
    @Scheduled(fixedDelay = 10000)
    public void autoAssignPatientsToRoomsForAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();

        tenants.parallelStream().forEach(tenant -> {
            String tenantCode = tenant.getCode();
            MDC.put("tenant", tenantCode);

            try {
                TenantContext.setTenantId(tenantCode);
                log.info("Bắt đầu xử lý tenant: {}", tenantCode);

                if (initializedTenants.contains(tenantCode)) {
                    // Nếu đã khởi tạo hệ thống phòng khám, chỉ dispatch thêm bệnh nhân mới
                    dispatchNewPatients(tenantCode);
                } else {
                    // Nếu tenant mới, khởi tạo hệ thống phòng khám ban đầu
                    initializeRoomSystem(tenantCode);
                }
            } catch (Exception e) {
                log.error("Lỗi xử lý tenant {}: {}", tenantCode, e.getMessage(), e);
            } finally {
                MDC.remove("tenant"); // tránh rò rỉ
                TenantContext.clear();
            }
        });
    }

    /**
     * Khởi tạo hệ thống phòng khám cho tenant mới:
     * - Tạo queue, lock, room manager, set xử lý
     * - Ưu tiên thêm lại các bệnh nhân IN_PROGRESS còn đang khám
     * - Nếu còn slot, thêm các bệnh nhân WAITING vào hàng đợi
     * - Khởi tạo ROOM_CAPACITY thread RoomWorker
     */
    private void initializeRoomSystem(String tenantCode) {
        log.info("Khởi tạo hệ thống phòng khám cho tenant: {}", tenantCode);

        Queue<QueuePatientsResponse> waitingQueue = new LinkedList<>();
        Object syncLock = new Object();
        RoomManager roomManager = new RoomManager(ROOM_CAPACITY);
        Set<String> processingSet = ConcurrentHashMap.newKeySet();

        tenantQueues.put(tenantCode, waitingQueue);
        tenantLocks.put(tenantCode, syncLock);
        tenantRoomManagers.put(tenantCode, roomManager);
        processingPatientIdsMap.put(tenantCode, processingSet);

        List<QueuePatientsResponse> allPatients = fetchPatientsWaitingOrInProgress();
        int roomAvailable = roomManager.getAvailableRoomCount();
        int addedTotal = 0;

        synchronized (syncLock) {
            // Ưu tiên xử lý bệnh nhân đang IN_PROGRESS (đang khám dở)
            for (QueuePatientsResponse patient : allPatients) {
                if (addedTotal >= roomAvailable) break;
                if (processingSet.contains(patient.getId())) continue;

                if (Status.IN_PROGRESS.name().equalsIgnoreCase(patient.getStatus())) {
                    waitingQueue.add(patient);
                    processingSet.add(patient.getId());
                    addedTotal++;
                    log.info("Thêm lại bệnh nhân {} (IN_PROGRESS) vào hàng đợi của tenant {}", patient.getPatientId(), tenantCode);
                }
            }

            // Sau đó nếu còn phòng trống thì thêm bệnh nhân mới đang WAITING
            for (QueuePatientsResponse patient : allPatients) {
                if (addedTotal >= roomAvailable) break;
                if (processingSet.contains(patient.getId())) continue;

                if (Status.WAITING.name().equalsIgnoreCase(patient.getStatus())) {
                    waitingQueue.add(patient);
                    processingSet.add(patient.getId());
                    addedTotal++;
                    log.info("Thêm bệnh nhân {} (WAITING) vào hàng đợi của tenant {}", patient.getPatientId(), tenantCode);
                }
            }
        }

        // Khởi tạo các RoomWorker tương ứng với số lượng phòng
        for (int i = 0; i < ROOM_CAPACITY; i++) {
            new RoomWorker(queuePatientsService, i, waitingQueue, syncLock, roomManager, tenantCode, processingPatientIdsMap).start();
        }

        initializedTenants.add(tenantCode);
    }

    /**
     * Gửi thêm bệnh nhân mới có trạng thái WAITING vào hàng đợi nếu còn phòng
     */
    private void dispatchNewPatients(String tenantCode) {
        Queue<QueuePatientsResponse> queue = tenantQueues.get(tenantCode);
        Object lock = tenantLocks.get(tenantCode);
        RoomManager roomManager = tenantRoomManagers.get(tenantCode);
        Set<String> processingSet = processingPatientIdsMap.get(tenantCode);

        if (queue == null || lock == null || roomManager == null || processingSet == null) {
            log.warn("Không tìm thấy context cho tenant {}, bỏ qua", tenantCode);
            return;
        }

        List<QueuePatientsResponse> waitingPatients = queuePatientsService.getAllQueuePatientsByStatusAndQueueId(
                Status.WAITING.name(),
                dailyQueueService.getActiveQueueIdForToday()
        );

        int roomAvailable = roomManager.getAvailableRoomCount();
        int added = 0;

        synchronized (lock) {
            for (QueuePatientsResponse newPatient : waitingPatients) {
                if (added >= roomAvailable) break;
                if (!processingSet.contains(newPatient.getId())) {
                    queue.add(newPatient);
                    processingSet.add(newPatient.getId());
                    added++;
                    log.info("Thêm bệnh nhân {} (WAITING) vào hàng đợi của tenant {}", newPatient.getPatientId(), tenantCode);
                }
            }

            // Nếu có bệnh nhân mới, đánh thức các RoomWorker đang chờ
            if (added > 0) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Trả về danh sách bệnh nhân có trạng thái WAITING hoặc IN_PROGRESS
     * để phục vụ khởi tạo hoặc khôi phục sau khi restart hệ thống
     */
    private List<QueuePatientsResponse> fetchPatientsWaitingOrInProgress() {
        String queueId = dailyQueueService.getActiveQueueIdForToday();
        List<QueuePatientsResponse> list = new ArrayList<>();
        list.addAll(queuePatientsService.getAllQueuePatientsByStatusAndQueueId(Status.WAITING.name(), queueId));
        list.addAll(queuePatientsService.getAllQueuePatientsByStatusAndQueueId(Status.IN_PROGRESS.name(), queueId));
        return list;
    }

}
