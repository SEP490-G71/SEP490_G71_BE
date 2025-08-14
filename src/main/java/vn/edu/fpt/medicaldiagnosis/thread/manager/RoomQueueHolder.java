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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final Map<Integer, Queue<QueuePatientsResponse>> roomQueues = new HashMap<>();

    /**
     * RoomWorker (Thread xử lý khám bệnh) tương ứng với từng phòng.
     * Key = roomNumber.
     */
    private final Map<Integer, RoomWorker> roomWorkers = new HashMap<>();

    /**
     * Loại phòng (nội khoa, nhi khoa...) tương ứng với mỗi roomNumber.
     * Dùng để lọc phòng phù hợp khi phân bệnh nhân (findLeastBusyRoom).
     */
    @Getter
    private final Map<Integer, DepartmentType> roomTypes = new HashMap<>();

    /**
     * Chuyên khoa tương ứng với mỗi phòng (key = roomNumber, value = specializationId).
     * Dùng để lọc phòng đúng chuyên môn khi phân bệnh nhân.
     */
    @Getter
    private final Map<Integer, String> roomSpecializations = new HashMap<>();

    /**
     * Sức chứa tối đa mỗi phòng (capacity) trong NGÀY.
     * Công thức thiết lập từ bên ngoài: capacity = countShiftsToday * docShiftQuota.
     * Nếu chưa set (null) → hiện tại mặc định không giới hạn (xem canAcceptNewPatient).
     */
    private final Map<Integer, Integer> roomCapacity = new HashMap<>();

    /**
     * Trạng thái quá tải theo phòng.
     * true  = đã đạt/ vượt capacity → không nhận thêm bệnh nhân.
     * false = còn chỗ.
     *
     * Giá trị này được cập nhật mỗi khi setCapacity/enqueue/refreshQueue.
     */
    private final Map<Integer, Boolean> roomOverloaded = new HashMap<>();

    /**
     * Comparator sắp xếp bệnh nhân trong hàng đợi theo mức độ ưu tiên (giá trị nhỏ hơn đứng trước):
     * -1. Đã khám xong (DONE) hoặc huỷ (CANCELED) → đưa lên đầu để loại bỏ sớm.
     *  0. Đang chờ kết quả (AWAITING_RESULT).
     *  1. Đang được khám (IN_PROGRESS).
     *  2. Đang gọi vào phòng (CALLING).
     *  3. Đang chờ (WAITING).
     *  4. Trạng thái khác (nếu có).
     *
     * Trong cùng một nhóm score:
     * - Bệnh nhân có `isPriority = true` được ưu tiên hơn.
     * - Sắp xếp theo thứ tự `queueOrder` tăng dần.
     *
     * Lưu ý: PriorityQueue ưu tiên giá trị nhỏ hơn → DONE/CANCELED = -1 giúp loại bỏ sớm.
     */
    private final Comparator<QueuePatientsResponse> priorityComparator = Comparator
            .comparingInt((QueuePatientsResponse p) -> {
                String status = p.getStatus();

                if (Status.CALLING.name().equalsIgnoreCase(status)) return 1;

                if (Status.WAITING.name().equalsIgnoreCase(status)) return 2;

                return 3; // fallback
            })
            .thenComparing(QueuePatientsResponse::getIsPriority, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(QueuePatientsResponse::getQueueOrder, Comparator.nullsLast(Long::compareTo));

    /**
     * Thread pool dùng để chạy RoomWorker tương ứng cho từng phòng.
     * Dùng cached thread pool để tái sử dụng thread, phù hợp với số phòng biến thiên.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Lock để đồng bộ thao tác khởi tạo hoặc truy cập vào roomQueues/roomWorkers/capacity...
     * Các thao tác trên từng queue sẽ đồng bộ RIÊNG trên đối tượng queue để giảm vùng khoá.
     */
    private final Object roomQueueLock = new Object();

    /**
     * Lock để đồng bộ thao tác khởi tạo RoomWorker hoặc stopAllWorkers.
     */
    private final Object workerLock = new Object();

    // ===================== HÀM QUẢN LÝ CAPACITY & OVERLOAD =====================

    /**
     * (Bổ sung) Cho phép đọc danh sách capacity theo phòng dạng read-only.
     */
    public Map<Integer, Integer> getCapacities() {
        synchronized (roomQueueLock) {
            return Map.copyOf(roomCapacity);
        }
    }

    /**
     * Cài đặt sức chứa tối đa cho một phòng.
     * @param roomNumber phòng cần set capacity
     * @param capacity   sức chứa tối đa (>= 0)
     *
     * Gọi hàm này sau khi đã tính: capacity = countShiftsToday * docShiftQuota.
     * Sau khi set, hệ thống sẽ tự cập nhật trạng thái quá tải (roomOverloaded).
     */
    public void setCapacity(int roomNumber, int capacity) {
        synchronized (roomQueueLock) {
            roomCapacity.put(roomNumber, Math.max(0, capacity)); // bảo vệ khỏi giá trị âm
            updateOverloadState(roomNumber);
        }
    }

    /**
     * Kiểm tra phòng có đang quá tải hay không (>= capacity).
     * @param roomNumber phòng cần kiểm tra
     * @return true nếu quá tải, false nếu bình thường hoặc chưa set capacity.
     */
    public boolean isOverloaded(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomOverloaded.getOrDefault(roomNumber, false);
        }
    }

    /**
     * Kiểm tra phòng có thể nhận thêm bệnh nhân hay không.
     * @param roomNumber phòng cần kiểm tra
     * @return true nếu:
     *  - chưa set capacity (cap == null) → hiện tại cho phép (mặc định "không giới hạn")
     *  - hoặc đã set capacity và số lượng hiện tại < capacity
     *
     * NOTE (strict mode): nếu muốn phòng "mồ côi" (chưa set capacity) KHÔNG nhận thêm bệnh nhân,
     * đổi dòng `if (cap == null) return true;` thành `if (cap == null) return false;`
     * để đảm bảo không có phòng có capacity "vô hạn" do quên set.
     */
    public boolean canAcceptNewPatient(int roomNumber) {
        synchronized (roomQueueLock) {
            Integer cap = roomCapacity.get(roomNumber);
            Queue<QueuePatientsResponse> q = roomQueues.get(roomNumber);
            if (cap == null) return true;
            int current = (q != null) ? q.size() : 0;
            return current < cap;
        }
    }

    /**
     * Cập nhật trạng thái quá tải (overloaded) cho 1 phòng dựa trên capacity & kích thước queue hiện tại.
     * Gọi mỗi khi:
     * - setCapacity
     * - enqueue
     * - refreshQueue
     */
    private void updateOverloadState(int roomNumber) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> q = roomQueues.get(roomNumber);
            Integer cap = roomCapacity.get(roomNumber);
            boolean overloaded = (cap != null) && (q != null) && (q.size() >= cap);
            roomOverloaded.put(roomNumber, overloaded);
        }
    }

    // ===================== HÀM XỬ LÝ =====================

    /**
     * Khởi tạo phòng:
     * - Tạo queue nếu chưa có.
     * - Tạo và khởi động RoomWorker nếu chưa có.
     * - Phục hồi các bệnh nhân đã gán vào phòng từ DB (nếu có queueId).
     * - KHÔNG set capacity ở đây — capacity được set từ AutoRoomAssignmentJob sau khi tính toán.
     */
    public void initRoom(int roomNumber, String tenantCode, QueuePatientsService queuePatientsService, String queueId, TextToSpeechService ttsService, PatientRepository patientRepository) {
        Queue<QueuePatientsResponse> queue;
        synchronized (roomQueueLock) {
            // Tạo mới priority queue nếu phòng chưa có
            queue = roomQueues.computeIfAbsent(roomNumber, id -> new PriorityQueue<>(priorityComparator));
        }
        synchronized (workerLock) {
            // Khởi động RoomWorker nếu chưa tồn tại
            if (!roomWorkers.containsKey(roomNumber)) {
                RoomWorker worker = new RoomWorker(roomNumber, tenantCode, queue, queuePatientsService, ttsService, patientRepository);
                roomWorkers.put(roomNumber, worker);
                executor.submit(worker);
            }
        }
        // Phục hồi các bệnh nhân đã gán phòng (nếu có)
        if (queueId != null) {
            List<QueuePatientsResponse> list = queuePatientsService.getAssignedPatientsForRoom(queueId, String.valueOf(roomNumber));
            synchronized (queue) {
                queue.addAll(list);
            }
            // Sau khi phục hồi từ DB, cập nhật luôn trạng thái quá tải hiện tại
            updateOverloadState(roomNumber);
        }
    }

    /**
     * Đăng ký metadata cho phòng (type & specialization).
     * - Dùng trong findLeastBusyRoom để chọn phòng phù hợp.
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
     * Thêm bệnh nhân vào hàng đợi của phòng (nếu chưa trùng ID trong queue) và cập nhật trạng thái quá tải.
     * - Nếu phòng đã đầy (>= capacity) → không enque và đánh dấu overloaded = true.
     * - Đặt assignedTime = now khi enqueue thành công.
     */
    public void enqueue(int roomNumber, QueuePatientsResponse patient) {
        synchronized (roomQueueLock) {
            Queue<QueuePatientsResponse> queue = roomQueues.get(roomNumber);
            if (queue != null) {
                synchronized (queue) {
                    // Chặn thêm bệnh nhân nếu quá tải theo capacity hiện tại
                    if (!canAcceptNewPatient(roomNumber)) {
                        roomOverloaded.put(roomNumber, true);
                        return;
                    }
                    // Tránh thêm trùng 1 bệnh nhân nhiều lần trong queue
                    if (queue.stream().noneMatch(p -> p.getId().equals(patient.getId()))) {
                        patient.setAssignedTime(LocalDateTime.now());
                        queue.offer(patient);
                        // cập nhật trạng thái quá tải sau khi thêm
                        updateOverloadState(roomNumber);
                    }
                }
            }
        }
    }

    /**
     * Trả về queue của một phòng (có thể null nếu phòng chưa init).
     * - CHỈ đọc: nếu cần chỉnh sửa queue, hãy đồng bộ trên chính đối tượng queue.
     */
    public Queue<QueuePatientsResponse> getQueue(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.get(roomNumber);
        }
    }

    /**
     * Kiểm tra phòng đã được init hay chưa (tồn tại queue).
     */
    public boolean hasRoom(int roomNumber) {
        synchronized (roomQueueLock) {
            return roomQueues.containsKey(roomNumber);
        }
    }

    /**
     * Tìm phòng phù hợp (theo DepartmentType và specialization) có số bệnh nhân ÍT NHẤT và CÒN CHỖ.
     * Ưu tiên theo thứ tự:
     * 1) có type & specialization khớp
     * 2) chỉ khớp type
     * 3) không có phòng phù hợp → trả về null
     */
    public Integer findLeastBusyRoom(DepartmentType type, String specializationId) {
        synchronized (roomQueueLock) {
            List<Map.Entry<Integer, Queue<QueuePatientsResponse>>> candidates;

            // Case 1: type + specialization match
            if (type != null && specializationId != null) {
                candidates = roomQueues.entrySet().stream()
                        .filter(e -> type.equals(roomTypes.get(e.getKey())))
                        .filter(e -> specializationId.equals(roomSpecializations.get(e.getKey())))
                        .filter(e -> canAcceptNewPatient(e.getKey())) // chỉ lấy phòng còn chỗ
                        .toList();

                if (!candidates.isEmpty()) {
                    return getLeastBusyRoomFromCandidates(candidates);
                }
            }

            // Case 2: fallback with only type
            if (type != null) {
                candidates = roomQueues.entrySet().stream()
                        .filter(e -> type.equals(roomTypes.get(e.getKey())))
                        .filter(e -> canAcceptNewPatient(e.getKey()))
                        .toList();

                if (!candidates.isEmpty()) {
                    return getLeastBusyRoomFromCandidates(candidates);
                }
            }

            // Case 3: No match at all
            return null;
        }
    }

    /**
     * Từ danh sách phòng ứng viên, chọn phòng có queue size nhỏ nhất.
     * Nếu có nhiều phòng cùng size → chọn roomNumber nhỏ nhất.
     */
    private Integer getLeastBusyRoomFromCandidates(List<Map.Entry<Integer, Queue<QueuePatientsResponse>>> candidates) {
        int minSize = candidates.stream().mapToInt(e -> e.getValue().size()).min().orElse(Integer.MAX_VALUE);

        return candidates.stream()
                .filter(e -> e.getValue().size() == minSize)
                .map(Map.Entry::getKey)
                .sorted() // Ưu tiên phòng có số phòng nhỏ hơn khi size bằng nhau
                .findFirst()
                .orElse(null);
    }

    /**
     * Dừng toàn bộ RoomWorker (khi shutdown ứng dụng hoặc tenant stop).
     * - Gửi tín hiệu stopWorker cho từng worker.
     * - shutdownNow() thread pool để hủy các nhiệm vụ còn lại.
     */
    public void stopAllWorkers() {
        synchronized (workerLock) {
            roomWorkers.values().forEach(RoomWorker::stopWorker);
        }
        executor.shutdownNow();
    }

    /**
     * Thêm bệnh nhân vào queue và chạy callback ngay sau đó (thường để notify UI/WebSocket, refresh view).
     * - Callback được gọi DÙ enqueue có thành công hay không (tuỳ vào logic hiện tại).
     *   Nếu muốn chỉ gọi khi enqueue thành công, bạn có thể đổi sang:
     *     if (enqueueSuccessful) callback.run();
     */
    public void enqueuePatientAndNotifyListeners(int roomNumber, QueuePatientsResponse patient, Runnable notifyListenersCallback) {
        enqueue(roomNumber, patient);
        if (notifyListenersCallback != null) {
            notifyListenersCallback.run();
        }
    }

    /**
     * Làm mới queue: tải lại bản ghi mới nhất từ DB cho các bệnh nhân trong queue hiện tại
     * → clear queue → offer lại → cập nhật trạng thái quá tải.
     *
     * Chức năng:
     * - Giảm rủi ro stale data (ví dụ status thay đổi) khi worker/luồng khác đã cập nhật DB.
     * - Giữ lại thứ tự ưu tiên nhờ cùng comparator.
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

                // Sau khi refresh lại queue → cập nhật overloaded theo capacity hiện tại
                updateOverloadState(roomNumber);
            }
        }
    }
}
