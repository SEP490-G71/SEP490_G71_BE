package vn.edu.fpt.medicaldiagnosis.thread.manager;

import lombok.Getter;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.TextToSpeechService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Quản lý các hàng đợi và RoomWorker tương ứng cho từng phòng khám.
 * - Mỗi phòng có một PriorityQueue<QueuePatientsResponse> để xếp thứ tự ưu tiên bệnh nhân.
 * - Mỗi phòng có một RoomWorker (Thread) xử lý logic gọi khám/tiến trình khám.
 * - Hỗ trợ "sức chứa" (capacity) theo phòng và "trạng thái quá tải" (overloaded).
 *
 * Đồng bộ:
 * - roomQueueLock dùng để bảo vệ cấu trúc roomQueues/roomWorkers/... (tạo phòng, kiểm tra tồn tại).
 * - Đồng bộ riêng trên từng queue khi thao tác enque/deque để giảm vùng lock (giảm contention).
 *
 * Lưu ý:
 * - Capacity được set từ bên ngoài (AutoRoomAssignmentJob) dựa trên: capacity = countShiftsToday * docShiftQuota.
 * - Nếu chưa set capacity (cap == null), hiện tại coi như KHÔNG GIỚI HẠN (có thể đổi sang strict mode nếu muốn).
 */
public class RoomQueueHolder {

    // ===================== CÁC BIẾN TOÀN CỤC =====================

    /**
     * Hàng đợi bệnh nhân theo phòng, key = roomNumber.
     * Mỗi queue là PriorityQueue với comparator ưu tiên theo trạng thái + isPriority + queueOrder.
     */
    // NEW: thay HashMap bằng ConcurrentHashMap để giảm lock toàn cục
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new ConcurrentHashMap<>();

    /**
     * RoomWorker (Thread xử lý khám bệnh) tương ứng với từng phòng.
     * Key = roomNumber.
     */
    private final Map<Integer, RoomWorker> roomWorkers = new ConcurrentHashMap<>();

    /**
     * Loại phòng (nội khoa, nhi khoa...) tương ứng với mỗi roomNumber.
     * Dùng để lọc phòng phù hợp khi phân bệnh nhân (findLeastBusyRoom).
     */
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new ConcurrentHashMap<>();

    /**
     * Chuyên khoa tương ứng với mỗi phòng (key = roomNumber, value = specializationId).
     * Dùng để lọc phòng đúng chuyên môn khi phân bệnh nhân.
     */
    @Getter
    private final Map<Integer, String> roomSpecializations = new ConcurrentHashMap<>();

    /**
     * Sức chứa tối đa mỗi phòng (capacity) trong NGÀY.
     * Công thức thiết lập từ bên ngoài: capacity = countShiftsToday * docShiftQuota.
     * Nếu chưa set (null) → hiện tại mặc định không giới hạn (xem canAcceptNewPatient).
     */
    private final Map<Integer, Integer> roomCapacity = new ConcurrentHashMap<>();

    /**
     * Trạng thái quá tải theo phòng.
     * true  = đã đạt/ vượt capacity → không nhận thêm bệnh nhân.
     * false = còn chỗ.
     *
     * Giá trị này được cập nhật mỗi khi setCapacity/enqueue/refreshQueue.
     */
    private final Map<Integer, Boolean> roomOverloaded = new ConcurrentHashMap<>();

    // NEW: lock riêng cho từng queue để tránh synchronized lồng nhau
    private final Map<Integer, ReentrantLock> queueLocks = new ConcurrentHashMap<>();

    /**
     * Comparator sắp xếp bệnh nhân trong hàng đợi theo mức độ ưu tiên.
     */
    private final Comparator<QueuePatientsResponse> priorityComparator = Comparator
            .comparingInt((QueuePatientsResponse p) -> {
                String status = p.getStatus();
                if (Status.IN_PROGRESS.name().equalsIgnoreCase(status)) return 0;
                if (Status.CALLING.name().equalsIgnoreCase(status)) return 1;
                if (Status.WAITING.name().equalsIgnoreCase(status)) return 2;
                return 3;
            })
            .thenComparing(QueuePatientsResponse::getIsPriority, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(QueuePatientsResponse::getQueueOrder, Comparator.nullsLast(Long::compareTo));

    /**
     * Thread pool dùng để chạy RoomWorker tương ứng cho từng phòng.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Lock để đồng bộ thao tác khởi tạo hoặc truy cập vào roomQueues/roomWorkers/capacity...
     */
    private final Object roomQueueLock = new Object();

    /**
     * Lock để đồng bộ thao tác khởi tạo RoomWorker hoặc stopAllWorkers.
     */
    private final Object workerLock = new Object();

    // ===================== HÀM QUẢN LÝ CAPACITY & OVERLOAD =====================

    public Map<Integer, Integer> getCapacities() {
        return Map.copyOf(roomCapacity);
    }

    public void setCapacity(int roomNumber, int capacity) {
        roomCapacity.put(roomNumber, Math.max(0, capacity));
        updateOverloadState(roomNumber);
    }

    public boolean isOverloaded(int roomNumber) {
        return roomOverloaded.getOrDefault(roomNumber, false);
    }

    public boolean canAcceptNewPatient(int roomNumber) {
        Integer cap = roomCapacity.get(roomNumber);
        Queue<QueuePatientsResponse> q = roomQueues.get(roomNumber);
        if (cap == null) return true;
        int current = (q != null) ? q.size() : 0;
        return current < cap;
    }

    private void updateOverloadState(int roomNumber) {
        Queue<QueuePatientsResponse> q = roomQueues.get(roomNumber);
        Integer cap = roomCapacity.get(roomNumber);
        boolean overloaded = (cap != null) && (q != null) && (q.size() >= cap);
        roomOverloaded.put(roomNumber, overloaded);
    }

    // ===================== HÀM XỬ LÝ =====================

    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService queuePatientsService,
                         String queueId, TextToSpeechService ttsService, PatientRepository patientRepository) {
        Queue<QueuePatientsResponse> queue = roomQueues.computeIfAbsent(roomNumber,
                id -> new PriorityQueue<>(priorityComparator));
        queueLocks.putIfAbsent(roomNumber, new ReentrantLock()); // NEW: lock riêng cho từng phòng

        synchronized (workerLock) {
            if (!roomWorkers.containsKey(roomNumber)) {
                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue,
                        queuePatientsService, ttsService, patientRepository);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker);
            }
        }

        if (queueId != null) {
            List<QueuePatientsResponse> list = queuePatientsService.getAssignedPatientsForRoom(queueId,
                    String.valueOf(roomNumber));
            ReentrantLock lock = queueLocks.get(roomNumber);
            lock.lock();
            try {
                queue.addAll(list);
                updateOverloadState(roomNumber);
            } finally {
                lock.unlock();
            }
        }
    }

    public void registerDepartmentMetadata(int roomNumber, DepartmentResponse department) {
        if (department.getType() != null) {
            roomTypes.put(roomNumber, department.getType());
        }
        if (department.getSpecialization() != null && department.getSpecialization().getId() != null) {
            roomSpecializations.put(roomNumber, department.getSpecialization().getId());
        }
    }

    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
        if (queue == null) return;

        ReentrantLock lock = queueLocks.get(roomNumber);
        lock.lock();
        try {
            if (!canAcceptNewPatient(roomNumber)) {
                roomOverloaded.put(roomNumber, true);
                return;
            }
            if (queue.stream().noneMatch(p -> p.getId().equals(patient.getId()))) {
                patient.setAssignedTime(LocalDateTime.now());
                queue.offer(patient);
                updateOverloadState(roomNumber);
            }
        } finally {
            lock.unlock();
        }
    }

    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        return roomQueues.get(roomNumber);
    }

    public boolean hasRoom(int roomNumber) {
        return roomQueues.containsKey(roomNumber);
    }

    public Integer findLeastBusyRoom(DepartmentType type, String specializationId) {
        List<Map.Entry<Integer, Queue<QueuePatientsResponse>>> candidates;

        if (type != null && specializationId != null) {
            candidates = roomQueues.entrySet().stream()
                    .filter(e -> type.equals(roomTypes.get(e.getKey())))
                    .filter(e -> specializationId.equals(roomSpecializations.get(e.getKey())))
                    .filter(e -> canAcceptNewPatient(e.getKey()))
                    .toList();
            if (!candidates.isEmpty()) {
                return getLeastBusyRoomFromCandidates(candidates);
            }
        }

        if (type != null) {
            candidates = roomQueues.entrySet().stream()
                    .filter(e -> type.equals(roomTypes.get(e.getKey())))
                    .filter(e -> canAcceptNewPatient(e.getKey()))
                    .toList();
            if (!candidates.isEmpty()) {
                return getLeastBusyRoomFromCandidates(candidates);
            }
        }
        return null;
    }

    private Integer getLeastBusyRoomFromCandidates(List<Map.Entry<Integer, Queue<QueuePatientsResponse>>> candidates) {
        Map<Integer, Long> activeCounts = candidates.stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(p -> {
                                    String s = p.getStatus();
                                    return Status.WAITING.name().equalsIgnoreCase(s)
                                            || Status.CALLING.name().equalsIgnoreCase(s)
                                            || Status.IN_PROGRESS.name().equalsIgnoreCase(s);
                                })
                                .count()
                ));
        long minSize = activeCounts.values().stream().mapToLong(Long::longValue).min().orElse(Long.MAX_VALUE);
        return activeCounts.entrySet().stream()
                .filter(e -> e.getValue() == minSize)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker);
        }
        executor.shutdownNow();
    }

    public void enqueuePatientAndNotifyListeners(int roomNumber, QueuePatientsResponse patient,
                                                 Runnable notifyListenersCallback) {
        enqueue(roomNumber, patient);
        if (notifyListenersCallback != null) {
            notifyListenersCallback.run();
        }
    }

    public void refreshQueue(int roomNumber, QueuePatientsService service) {
        Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
        if (queue == null) return;

        ReentrantLock lock = queueLocks.get(roomNumber);

        // NEW: copy id ra ngoài trước khi query DB để không giữ lock lâu
        List<String> ids;
        lock.lock();
        try {
            ids = queue.stream().map(QueuePatientsResponse::getId).toList();
        } finally {
            lock.unlock();
        }

        List<QueuePatientsResponse> refreshed = ids.stream()
                .map(service::getQueuePatientsById)
                .toList();

        lock.lock();
        try {
            queue.clear();
            refreshed.forEach(queue::offer);
            updateOverloadState(roomNumber);
        } finally {
            lock.unlock();
        }
    }
}
