package vn.edu.fpt.medicaldiagnosis.thread.manager;

import lombok.Getter;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quản lý các hàng đợi và worker tương ứng cho từng phòng khám.
 * Mỗi phòng có 1 thread riêng (RoomWorker) xử lý danh sách bệnh nhân đang chờ.
 */
public class RoomQueueHolder {

    // Danh sách hàng đợi bệnh nhân theo phòng (key: roomNumber)
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    // Danh sách các thread xử lý theo phòng
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    // Kiểu phòng khám (nội, nhi, xét nghiệm...) ứng với từng phòng
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new HashMap<>();

    private final Comparator<QueuePatientsResponse> priorityComparator = Comparator
            // 1. Ưu tiên bệnh nhân đã được gọi (calledTime != null)
            .comparing((QueuePatientsResponse p) -> p.getCalledTime() != null).reversed()

            // 2. Sau đó ưu tiên theo flag ưu tiên (isPriority)
            .thenComparing(QueuePatientsResponse::getIsPriority, Comparator.nullsLast(Comparator.reverseOrder()))

            // 3. Sau đó là thứ tự trong hàng đợi (queue_order)
            .thenComparing(QueuePatientsResponse::getQueueOrder, Comparator.nullsLast(Long::compareTo));

    // Thread pool để chạy các RoomWorker (dùng cached pool để tái sử dụng thread)
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Lock để đồng bộ thao tác với roomQueues
    private final Object roomQueueLock = new Object();

    // Lock để đồng bộ thao tác với roomWorkers
    private final Object workerLock = new Object();

    /**
     * Khởi tạo phòng nếu chưa tồn tại:
     * - Tạo hàng đợi bệnh nhân cho phòng đó
     * - Khởi chạy RoomWorker để xử lý hàng đợi
     * - Nếu có queueId, phục hồi danh sách bệnh nhân đang chờ từ DB
     */
    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService service, String queueId) {
        Queue<QueuePatientsResponse> queue;

        // B1: Tạo hàng đợi cho phòng nếu chưa có
        synchronized (roomQueueLock) {
            queue = roomQueues.computeIfAbsent(roomNumber, id -> new PriorityQueue<>(priorityComparator));
        }

        // B2: Tạo worker xử lý hàng đợi nếu chưa có
        synchronized (workerLock) {
            if (!roomWorkers.containsKey(roomNumber)) {
                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue, service);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker); // Gửi thread vào pool
            }
        }

        // B3: Phục hồi danh sách bệnh nhân đang chờ từ DB
        if (queueId != null && service != null) {
            List<QueuePatientsResponse> list = service.getAssignedPatientsForRoom(queueId, String.valueOf(roomNumber));
            synchronized (queue) {
                queue.addAll(list);
            }
        }
    }

    /**
     * Thêm bệnh nhân vào hàng đợi phòng tương ứng nếu chưa tồn tại trong đó.
     * Đảm bảo không chèn trùng ID bệnh nhân.
     */
    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue != null) {
                synchronized (queue) {
                    // Tránh thêm trùng bệnh nhân
                    if (queue.stream().noneMatch(p -> p.getId().equals(patient.getId()))) {
                        patient.setAssignedTime(LocalDateTime.now()); // mark thời điểm vào hàng đợi
                        queue.offer(patient);
                    }
                }
            }
        }
    }

    /**
     * Lấy danh sách hàng đợi bệnh nhân của một phòng
     */
    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomNumber);
        }
    }

    /**
     * Kiểm tra xem phòng đã được khởi tạo hàng đợi hay chưa
     */
    public boolean hasRoom(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.containsKey(roomNumber);
        }
    }

    /**
     * Tìm phòng thuộc loại chỉ định và đang có ít bệnh nhân nhất.
     * Dùng để tự động gán bệnh nhân vào phòng tối ưu.
     */
    public int findLeastBusyRoom(DepartmentType type) {
        synchronized (roomQueueLock) {
            return roomQueues.entrySet().stream()
                    .filter(e -> roomTypes.get(e.getKey()) == type) // Chỉ lọc phòng đúng loại
                    .min(Comparator.comparingInt(e -> e.getValue().size())) // Ưu tiên phòng ít bệnh nhân nhất
                    .map(Map.Entry::getKey)
                    .orElse(0); // Không tìm thấy phòng phù hợp → trả về 0
        }
    }

    /**
     * Dừng toàn bộ worker khi hệ thống tắt (thường gọi từ @PreDestroy).
     * Dừng thread bằng flag và shutdown thread pool.
     */
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker); // Gửi tín hiệu dừng từng worker
        }
        executor.shutdownNow(); // Dừng thread pool ngay lập tức
    }

    public void enqueuePatientAndNotifyListeners(
            int roomNumber,
            QueuePatientsResponse patient,
            Runnable notifyListenersCallback
    ) {
        // Đưa bệnh nhân vào hàng đợi phòng
        enqueue(roomNumber, patient);

        // Gọi hàm notify sau khi enqueue xong
        if (notifyListenersCallback != null) {
            notifyListenersCallback.run();
        }
    }

    public void refreshQueue(int roomNumber, QueuePatientsService service) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue == null) return;

            synchronized (queue) {
                List<QueuePatientsResponse> refreshed = queue.stream()
                        .map(p -> service.getQueuePatientsById(p.getId()))
                        .toList();

                queue.clear();
                refreshed.forEach(queue::offer);
            }
        }
    }

}
