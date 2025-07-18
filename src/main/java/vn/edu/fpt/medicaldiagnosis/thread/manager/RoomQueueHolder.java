package vn.edu.fpt.medicaldiagnosis.thread.manager;

import lombok.Getter;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quản lý các hàng đợi và RoomWorker tương ứng cho từng phòng khám.
 * Mỗi phòng được gán một PriorityQueue chứa bệnh nhân và một luồng riêng để xử lý gọi khám tuần tự.
 */
public class RoomQueueHolder {

    // ===================== CÁC BIẾN TOÀN CỤC =====================

    /**
     * Hàng đợi bệnh nhân theo phòng. Mỗi phòng là một PriorityQueue, key là roomNumber.
     */
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    /**
     * RoomWorker (Thread xử lý khám bệnh) tương ứng với từng phòng.
     */
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    /**
     * Loại phòng (nội khoa, nhi khoa...) tương ứng với mỗi roomNumber.
     */
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new HashMap<>();

    /**
     * Chuyên khoa tương ứng với mỗi phòng (key = roomNumber, value = specializationId).
     */
    @Getter
    private final Map<Integer, String> roomSpecializations = new HashMap<>();

    /**
     * Comparator sắp xếp bệnh nhân trong hàng đợi theo mức độ ưu tiên:
     * Ưu tiên theo thứ tự:
     * 1. Đã khám xong (DONE) hoặc huỷ (CANCELED) → loại bỏ sớm (score thấp nhất -1).
     * 2. Đang được khám (IN_PROGRESS) → ưu tiên cao nhất (score = 0).
     * 3. Đang chờ khám (WAITING) + đã được gọi → tiếp theo (score = 1).
     * 4. Đang chờ khám (WAITING) + chưa được gọi → tiếp theo (score = 2).
     * 5. Trạng thái khác → thấp nhất (score = 3).
     *
     * Trong cùng một nhóm score:
     * - Bệnh nhân có `isPriority = true` được ưu tiên hơn.
     * - Sắp xếp theo thứ tự `queueOrder` tăng dần.
     */
    private final Comparator<QueuePatientsResponse> priorityComparator = Comparator
            .comparingInt((QueuePatientsResponse p) -> {
                String status = p.getStatus();
                if (Status.DONE.name().equalsIgnoreCase(status) || Status.CANCELED.name().equalsIgnoreCase(status)) return -1;
                if (Status.IN_PROGRESS.name().equalsIgnoreCase(status)) return 0;
                if (Status.WAITING.name().equalsIgnoreCase(status)) {
                    return (p.getCalledTime() != null) ? 1 : 2;
                }
                return 3;
            })
            .thenComparing(QueuePatientsResponse::getIsPriority, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(QueuePatientsResponse::getQueueOrder, Comparator.nullsLast(Long::compareTo));

    /**
     * Thread pool dùng để chạy RoomWorker tương ứng cho từng phòng.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Lock để đồng bộ thao tác khởi tạo hoặc truy cập vào roomQueues.
     */
    private final Object roomQueueLock = new Object();

    /**
     * Lock để đồng bộ thao tác khởi tạo RoomWorker hoặc stopAllWorkers.
     */
    private final Object workerLock = new Object();

    // ===================== HÀM XỬ LÝ =====================

    /**
     * Khởi tạo phòng khám:
     * - Tạo queue nếu chưa có.
     * - Khởi động worker nếu chưa có.
     * - Phục hồi danh sách bệnh nhân đã gán vào phòng từ DB nếu có queueId.
     *
     * @param roomNumber   Mã số phòng khám (ví dụ: 101, 301).
     * @param tenantCode   Mã tenant đang sử dụng hệ thống (đa tenant).
     * @param service      Service thao tác với bảng queue_patients.
     * @param queueId      ID hàng đợi đang hoạt động trong ngày (nếu null thì không phục hồi).
     */
    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService service, String queueId) {
        Queue<QueuePatientsResponse> queue;

        synchronized (roomQueueLock) {
            queue = roomQueues.computeIfAbsent(roomNumber, id -> new PriorityQueue<>(priorityComparator));
        }

        synchronized (workerLock) {
            if (!roomWorkers.containsKey(roomNumber)) {
                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue, service);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker);
            }
        }

        if (queueId != null && service != null) {
            List<QueuePatientsResponse> list = service.getAssignedPatientsForRoom(queueId, String.valueOf(roomNumber));
            synchronized (queue) {
                queue.addAll(list);
            }
        }
    }

    /**
     * Đăng ký metadata cho phòng: loại phòng và chuyên khoa (nếu có).
     *
     * @param roomNumber Mã số phòng.
     * @param department Thông tin phòng từ response.
     */
    public void registerDepartmentMetadata(int roomNumber, DepartmentResponse department) {
        if (department.getType() != null) {
            roomTypes.put(roomNumber, department.getType());
        }
        if (department.getSpecialization() != null && department.getSpecialization().getId() != null) {
            roomSpecializations.put(roomNumber, department.getSpecialization().getId());
        }
    }

    /**
     * Thêm bệnh nhân vào hàng đợi của phòng chỉ định, nếu chưa tồn tại.
     *
     * @param roomNumber  Mã số phòng.
     * @param patient     Thông tin bệnh nhân cần thêm vào hàng đợi.
     */
    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue != null) {
                synchronized (queue) {
                    if (queue.stream().noneMatch(p -> p.getId().equals(patient.getId()))) {
                        patient.setAssignedTime(LocalDateTime.now());
                        queue.offer(patient);
                    }
                }
            }
        }
    }

    /**
     * Trả về hàng đợi (Queue) của một phòng đã khởi tạo.
     *
     * @param roomNumber Mã số phòng.
     * @return Queue của bệnh nhân đang chờ trong phòng, hoặc null nếu chưa có.
     */
    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomNumber);
        }
    }

    /**
     * Kiểm tra xem một phòng đã được khởi tạo hay chưa.
     *
     * @param roomNumber Mã số phòng.
     * @return true nếu đã tồn tại, false nếu chưa.
     */
    public boolean hasRoom(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.containsKey(roomNumber);
        }
    }

    /**
     * Tìm phòng có DepartmentType và Specialization phù hợp, ít bệnh nhân nhất.
     * Ưu tiên theo thứ tự:
     * 1. Phòng cùng loại và đúng chuyên khoa.
     * 2. Phòng cùng loại (bất kỳ chuyên khoa).
     * 3. Nếu không có → return null.
     *
     * @param type Loại phòng cần tìm.
     * @param specializationId ID chuyên khoa cần tìm (có thể null).
     * @return roomNumber phù hợp hoặc null nếu không tìm thấy.
     */
    public Integer findLeastBusyRoom(DepartmentType type, String specializationId) {
        synchronized (roomQueueLock) {
            // Ưu tiên phòng cùng loại và đúng chuyên khoa
            Optional<Integer> matchedSpecialization = roomQueues.entrySet().stream()
                    .filter(e -> type != null && type.equals(roomTypes.get(e.getKey())))
                    .filter(e -> specializationId != null && specializationId.equals(roomSpecializations.get(e.getKey())))
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey);

            return matchedSpecialization.orElseGet(() -> roomQueues.entrySet().stream()
                    .filter(e -> type != null && type.equals(roomTypes.get(e.getKey())))
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }
    }

    /**
     * Gửi tín hiệu dừng toàn bộ RoomWorker khi hệ thống shutdown hoặc cần khởi động lại.
     */
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker);
        }
        executor.shutdownNow();
    }

    /**
     * Thêm bệnh nhân vào hàng đợi của phòng và thực thi callback ngay sau đó
     * (dùng để cập nhật WebSocket, UI, hoặc refresh danh sách hiển thị).
     *
     * @param roomNumber              Phòng cần thêm bệnh nhân.
     * @param patient                 Bệnh nhân cần thêm.
     * @param notifyListenersCallback Callback để gọi sau khi enqueue (có thể null).
     */
    public void enqueuePatientAndNotifyListeners(
            int roomNumber,
            QueuePatientsResponse patient,
            Runnable notifyListenersCallback
    ) {
        enqueue(roomNumber, patient);
        if (notifyListenersCallback != null) {
            notifyListenersCallback.run();
        }
    }

    /**
     * Làm mới hàng đợi của phòng chỉ định bằng cách lấy dữ liệu mới nhất từ DB,
     * sắp xếp lại theo logic ưu tiên và thay thế danh sách cũ.
     *
     * @param roomNumber Mã số phòng cần làm mới.
     * @param service    Service truy xuất DB để lấy lại QueuePatientsResponse.
     */
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
