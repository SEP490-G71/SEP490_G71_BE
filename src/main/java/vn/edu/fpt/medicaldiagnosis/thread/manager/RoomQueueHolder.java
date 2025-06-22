package vn.edu.fpt.medicaldiagnosis.thread.manager;

import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomQueueHolder {

    // Tổng số phòng hoạt động (mỗi phòng tương ứng 1 thread)
    private final int roomCapacity;

    // Map lưu danh sách bệnh nhân xếp hàng theo từng phòng
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    // Map lưu thông tin các worker đang xử lý bệnh nhân trong từng phòng
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    // Object khóa dùng để đồng bộ hóa roomQueues
    private final Object roomQueueLock = new Object();

    // Object khóa dùng để đồng bộ hóa roomWorkers
    private final Object workerLock = new Object();

    // Thread pool dùng để chạy các RoomWorker
    private final ExecutorService executor;

    // Constructor: khởi tạo số lượng phòng và queue tương ứng cho mỗi phòng
    public RoomQueueHolder(int roomCapacity) {
        this.roomCapacity = roomCapacity;
        this.executor = Executors.newFixedThreadPool(roomCapacity);

        for (int i = 0; i < roomCapacity; i++) {
            roomQueues.put(i, new LinkedList<>());
        }
    }

    // Thêm bệnh nhân vào hàng đợi của phòng chỉ định (có đồng bộ)
    public void enqueue(int roomId, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            roomQueues.get(roomId).offer(patient);
        }
    }

    // Lấy hàng đợi bệnh nhân của một phòng (có đồng bộ)
    public Queue<QueuePatientsResponse> getQueue(int roomId) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomId);
        }
    }

    // Tìm phòng có ít bệnh nhân nhất để phân bệnh nhân mới vào (có đồng bộ)
    public int findLeastBusyRoom() {
        synchronized (roomQueueLock) {
            return roomQueues.entrySet().stream()
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(0);
        }
    }

    // Bắt đầu chạy các RoomWorker xử lý bệnh nhân cho từng phòng
    public void startWorkers(String tenantCode, QueuePatientsService service) {
        synchronized (workerLock) {
            for (int i = 0; i < roomCapacity; i++) {
                Queue<QueuePatientsResponse> queue;
                synchronized (roomQueueLock) {
                    queue = roomQueues.get(i); // lấy queue tương ứng của phòng
                }
                // Tạo RoomWorker mới cho phòng này
                RoomWorker worker = new RoomWorker(i, tenantCode, queue, service);
                roomWorkers.put(i, worker);
                executor.submit(worker); // gửi vào thread pool
            }
        }
    }

    // Dừng tất cả RoomWorker khi hệ thống tắt
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker); // gửi tín hiệu dừng
        }
        executor.shutdownNow(); // tắt thread pool ngay lập tức
    }
}
