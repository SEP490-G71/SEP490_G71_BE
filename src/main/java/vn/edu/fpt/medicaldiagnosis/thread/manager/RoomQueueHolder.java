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

    // Danh sách queue của từng phòng
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    // Danh sách worker theo phòng
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    // Danh sách loại phòng khám
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new HashMap<>();

    // Thread pool cho RoomWorker
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Khóa đồng bộ
    private final Object roomQueueLock = new Object();
    private final Object workerLock = new Object();

    /**
     * Khởi tạo một phòng nếu chưa tồn tại
     */
    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService service, String queueId) {
        synchronized (roomQueueLock) {
            roomQueues.computeIfAbsent(roomNumber, id -> new LinkedList<>());
        }

        synchronized (workerLock) {
            if (!roomWorkers.containsKey(roomNumber)) {
                Queue<QueuePatientsResponse> queue;
                synchronized (roomQueueLock) {
                    queue = roomQueues.get(roomNumber);
                }

                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue, service);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker);
            }
        }

        // Khôi phục bệnh nhân từ DB (nếu cần)
        if (queueId != null && service != null) {
            List<QueuePatientsResponse> list = service.getAssignedPatientsForRoom(queueId, String.valueOf(roomNumber));
            synchronized (roomQueueLock) {
                roomQueues.get(roomNumber).addAll(list);
            }
        }
    }

    /**
     * Thêm bệnh nhân vào hàng đợi
     */
    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue != null) {
                queue.offer(patient);
            }
        }
    }

    /**
     * Lấy hàng đợi của một phòng
     */
    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomNumber);
        }
    }

    /**
     * Kiểm tra phòng đã tồn tại chưa
     */
    public boolean hasRoom(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.containsKey(roomNumber);
        }
    }

    /**
     * Tìm phòng ít bệnh nhân nhất và lọc đúng loại phòng
     */
    public int findLeastBusyRoom(DepartmentType type) {
        synchronized (roomQueueLock) {
            return roomQueues.entrySet().stream()
                    .filter(e -> roomTypes.get(e.getKey()) == type)
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(0);
        }
    }

    /**
     * Dừng toàn bộ RoomWorker khi tắt hệ thống
     */
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker);
        }
        executor.shutdownNow();
    }

}
