package vn.edu.fpt.medicaldiagnosis.thread.manager;

import lombok.Getter;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomQueueHolder {

    // Danh sách hàng đợi theo phòng
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    // Danh sách luồng xử lý theo phòng
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    // 	Danh sách loại phòng khám ứng với từng phòng
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new HashMap<>();

    // Thread pool để chạy RoomWorker (mỗi phòng 1 thread riêng)
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Khóa đồng bộ cho thao tác trên roomQueues
    private final Object roomQueueLock = new Object();

    // Khóa đồng bộ cho thao tác trên roomWorkers
    private final Object workerLock = new Object();

    /**
     * Khởi tạo một phòng khám mới (nếu chưa tồn tại):
     * - Tạo hàng đợi tương ứng
     * - Tạo và chạy RoomWorker (thread) nếu chưa có
     * - Khôi phục lại bệnh nhân cũ (WAITING, IN_PROGRESS) từ DB nếu queueId được truyền
     */
    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService service, String queueId) {
        // Tạo hàng đợi nếu chưa tồn tại
        synchronized (roomQueueLock) {
            roomQueues.computeIfAbsent(roomNumber, id -> new LinkedList<>());
        }

        // Tạo và khởi chạy worker riêng cho phòng này nếu chưa tồn tại
        synchronized (workerLock) {
            if (!roomWorkers.containsKey(roomNumber)) {
                Queue<QueuePatientsResponse> queue;
                synchronized (roomQueueLock) {
                    queue = roomQueues.get(roomNumber); // đảm bảo thread-safe
                }

                // Worker gắn với phòng, tenant, queue và service
                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue, service);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker); // chạy thread worker
            }
        }

        // Khôi phục lại các bệnh nhân đang chờ khám hoặc đang khám từ DB vào hàng đợi (nếu có queueId)
        if (queueId != null && service != null) {
            List<QueuePatientsResponse> list = service.getAssignedPatientsForRoom(queueId, String.valueOf(roomNumber));
            synchronized (roomQueueLock) {
                roomQueues.get(roomNumber).addAll(list);
            }
        }
    }

    /**
     * Thêm bệnh nhân vào hàng đợi của phòng chỉ định
     */
    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue != null) {
                queue.offer(patient); // thêm vào cuối hàng đợi
            }
        }
    }

    /**
     * Lấy toàn bộ hàng đợi bệnh nhân của một phòng
     */
    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomNumber);
        }
    }

    /**
     * Kiểm tra một phòng đã được khởi tạo hay chưa (tức đã có queue)
     */
    public boolean hasRoom(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.containsKey(roomNumber);
        }
    }

    /**
     * Tìm phòng phù hợp với loại phòng (department type) và có ít bệnh nhân nhất
     * → dùng để gán bệnh nhân mới vào phòng hợp lý nhất
     */
    public int findLeastBusyRoom(DepartmentType type) {
        synchronized (roomQueueLock) {
            return roomQueues.entrySet().stream()
                    .filter(e -> roomTypes.get(e.getKey()) == type) // lọc đúng loại phòng
                    .min(Comparator.comparingInt(e -> e.getValue().size())) // ít bệnh nhân nhất
                    .map(Map.Entry::getKey)
                    .orElse(0); // nếu không tìm thấy thì trả về 0
        }
    }

    /**
     * Dừng toàn bộ worker khi hệ thống tắt (shutdown hook gọi từ @PreDestroy)
     */
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker); // dừng flag
        }
        executor.shutdownNow(); // huỷ tất cả thread
    }
}
