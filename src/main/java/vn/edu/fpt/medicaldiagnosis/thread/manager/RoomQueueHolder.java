package vn.edu.fpt.medicaldiagnosis.thread.manager;

import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.*;

public class RoomQueueHolder {
    private final int roomCapacity;
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new ConcurrentHashMap<>();
    private final Map<Integer, RoomWorker> roomWorkers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public RoomQueueHolder(int roomCapacity) {
        this.roomCapacity = roomCapacity;
        this.executor = Executors.newFixedThreadPool(roomCapacity);

        // Khởi tạo queue cho từng phòng
        for (int i = 0; i < roomCapacity; i++) {
            roomQueues.put(i, new ConcurrentLinkedQueue<>());
        }
    }

    public void enqueue(int roomId, QueuePatientsResponse patient) {
        roomQueues.get(roomId).offer(patient);
    }

    public Queue<QueuePatientsResponse> getQueue(int roomId) {
        return roomQueues.get(roomId);
    }

    // Trả về phòng có số lượng bệnh nhân ít nhất
    public int findLeastBusyRoom() {
        return roomQueues.entrySet().stream()
                .min(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    // Xử lý đa luồng các phòng
    public void startWorkers(String tenantCode, QueuePatientsService service) {
        for (int i = 0; i < roomCapacity; i++) {
            RoomWorker worker = new RoomWorker(i, tenantCode, getQueue(i), service);
            roomWorkers.put(i, worker);
            executor.submit(worker);
        }
    }

    // Dừng toàn bộ RoomWorker khi hệ thống shutdown
    public void stopAllWorkers() {
        roomWorkers.values().forEach(RoomWorker::stopWorker);
        executor.shutdownNow();
    }
}
