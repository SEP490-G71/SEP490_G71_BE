package vn.edu.fpt.medicaldiagnosis.thread.manager;

import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.util.*;
import java.util.concurrent.*;

public class RoomQueueHolder {
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new ConcurrentHashMap<>();
    private final int roomCapacity;

    public RoomQueueHolder(int roomCapacity) {
        this.roomCapacity = roomCapacity;
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

    public int findLeastBusyRoom() {
        return roomQueues.entrySet().stream()
                .min(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    public void startWorkers(String tenantCode, QueuePatientsService service) {
        for (int i = 0; i < roomCapacity; i++) {
            new Thread(new RoomWorker(i, tenantCode, getQueue(i), service), "RoomWorker-" + tenantCode + "-" + i).start();
        }
    }
}
