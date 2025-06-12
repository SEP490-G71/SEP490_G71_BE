package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
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

    // Số phòng khám mặc định cho mỗi tenant
    private static final int ROOM_CAPACITY = 5;

    // Các tenant đã được khởi tạo hệ thống phòng
    private final Set<String> initializedTenants = ConcurrentHashMap.newKeySet();

    // Map hàng đợi bệnh nhân đang chờ theo từng tenant
    private final Map<String, Queue<QueuePatientsResponse>> tenantQueues = new ConcurrentHashMap<>();

    // Map object dùng để đồng bộ hóa theo tenant
    private final Map<String, Object> tenantLocks = new ConcurrentHashMap<>();

    // Map quản lý trạng thái phòng theo tenant
    private final Map<String, RoomManager> tenantRoomManagers = new ConcurrentHashMap<>();

    // Map theo dõi các bệnh nhân đang được xử lý (để tránh lặp)
    private final Map<String, Set<String>> processingPatientIdsMap = new ConcurrentHashMap<>();

    /**
     * Hàm định kỳ (chạy mỗi 10s) thực hiện gán bệnh nhân đang chờ vào phòng khám tương ứng với từng tenant
     */
    @Scheduled(fixedDelay = 10000)
    public void autoAssignPatientsToRoomsForAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();

        tenants.parallelStream().forEach(tenant -> {
            String tenantCode = tenant.getCode();
            try {
                TenantContext.setTenantId(tenantCode);
                log.info("Bắt đầu xử lý tenant: {}", tenantCode);

                // Nếu đã khởi tạo, chỉ cần dispatch bệnh nhân mới
                if (initializedTenants.contains(tenantCode)) {
                    dispatchNewPatients(tenantCode);
                } else {
                    // Nếu chưa khởi tạo, tạo mới toàn bộ hệ thống phòng khám cho tenant đó
                    initializeRoomSystem(tenantCode);
                }
            } catch (Exception e) {
                log.error("Lỗi xử lý tenant {}: {}", tenantCode, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        });
    }

    /**
     * Khởi tạo hệ thống phòng khám cho một tenant mới
     */
    private void initializeRoomSystem(String tenantCode) {
        log.info("Khởi tạo hệ thống phòng khám cho tenant: {}", tenantCode);

        Queue<QueuePatientsResponse> waitingQueue = new LinkedList<>();
        Object syncLock = new Object();
        RoomManager roomManager = new RoomManager(ROOM_CAPACITY);
        Set<String> processingSet = ConcurrentHashMap.newKeySet();

        // Đưa vào các map lưu trữ theo từng tenant
        tenantQueues.put(tenantCode, waitingQueue);
        tenantLocks.put(tenantCode, syncLock);
        tenantRoomManagers.put(tenantCode, roomManager);
        processingPatientIdsMap.put(tenantCode, processingSet);

        // Lấy danh sách bệnh nhân đang chờ
        List<QueuePatientsResponse> initialPatients = fetchPatientsWaitingForExamination();
        int roomAvailable = roomManager.getAvailableRoomCount();
        int added = 0;

        // Thêm các bệnh nhân đang chờ khám vào hàng đợi ban đầu
        synchronized (syncLock) {
            for (QueuePatientsResponse newPatient : initialPatients) {
                if (added >= roomAvailable) break;
                if (!processingSet.contains(newPatient.getId())) {
                    waitingQueue.add(newPatient);
                    processingSet.add(newPatient.getId());
                    added++;
                    log.info("Thêm bệnh nhân {} vào hàng đợi của tenant {}", newPatient.getPatientId(), tenantCode);
                }
            }
        }

        // Khởi tạo các thread tương ứng với số lượng phòng
        for (int i = 0; i < ROOM_CAPACITY; i++) {
            new RoomWorker(queuePatientsService, i, waitingQueue, syncLock, roomManager, tenantCode, processingPatientIdsMap).start();
        }

        // Đánh dấu tenant đã được khởi tạo
        initializedTenants.add(tenantCode);
    }

    /**
     * Gửi thêm bệnh nhân mới vào hàng đợi cho tenant đã khởi tạo
     */
    private void dispatchNewPatients(String tenantCode) {
        Queue<QueuePatientsResponse> queue = tenantQueues.get(tenantCode);
        Object lock = tenantLocks.get(tenantCode);
        RoomManager roomManager = tenantRoomManagers.get(tenantCode);
        Set<String> processingSet = processingPatientIdsMap.get(tenantCode);

        // Kiểm tra tồn tại context đầy đủ
        if (queue == null || lock == null || roomManager == null || processingSet == null) {
            log.warn("Không tìm thấy context cho tenant {}, bỏ qua", tenantCode);
            return;
        }

        List<QueuePatientsResponse> waitingPatients = fetchPatientsWaitingForExamination();
        int roomAvailable = roomManager.getAvailableRoomCount();
        int added = 0;

        // Thêm bệnh nhân vào hàng đợi nếu còn phòng trống
        synchronized (lock) {
            for (QueuePatientsResponse newPatient : waitingPatients) {
                if (added >= roomAvailable) break;
                if (!processingSet.contains(newPatient.getId())) {
                    queue.add(newPatient);
                    processingSet.add(newPatient.getId());
                    added++;
                    log.info("Thêm bệnh nhân {} vào hàng đợi của tenant {}", newPatient.getPatientId(), tenantCode);
                }
            }
            if (added > 0) {
                lock.notifyAll(); // Đánh thức các RoomWorker nếu có thêm bệnh nhân mới
            }
        }
    }

    /**
     * Lấy tất cả bệnh nhân đang chờ khám (trạng thái WAITING)
     */
    private List<QueuePatientsResponse> fetchPatientsWaitingForExamination() {
        List<QueuePatientsResponse> list = queuePatientsService.getAllQueuePatientsByStatus(Status.WAITING.name());
        if (list.isEmpty()) {
            log.info("Không có bệnh nhân đang chờ khám.");
        }
        return list;
    }
}
