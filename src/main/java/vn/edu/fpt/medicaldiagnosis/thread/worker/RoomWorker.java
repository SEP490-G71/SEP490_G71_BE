package vn.edu.fpt.medicaldiagnosis.thread.worker;

import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;

import java.util.Queue;
import java.util.Random;

public class RoomWorker extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RoomWorker.class);

    private final int roomId;
    private final String roomName;
    private final Queue<Patient> cacheQueue;
    private final Object lock;
    private final RoomManager roomManager;

    public RoomWorker(int roomId, Queue<Patient> cacheQueue, Object lock, RoomManager roomManager) {
        this.roomId = roomId;
        this.roomName = "Phòng " + (roomId + 1);
        this.cacheQueue = cacheQueue;
        this.lock = lock;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        while (true) {
            Patient patient;

            // Chờ bệnh nhân nếu queue rỗng
            synchronized (lock) {
                while (cacheQueue.isEmpty()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Lấy bệnh nhân từ đầu hàng đợi và đánh dấu phòng bận
                patient = cacheQueue.poll();
                roomManager.markBusy(roomId, true);
            }

            log.info("{} bắt đầu khám cho bệnh nhân: {}", roomName, patient.getFullName());

            // Giả lập thời gian khám ngẫu nhiên (2-4 giây)
            try {
                Thread.sleep((new Random().nextInt(3) + 2) * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("{} hoàn tất khám cho bệnh nhân: {}", roomName, patient.getFullName());

            // Đánh dấu phòng rảnh sau khi khám xong
            roomManager.markBusy(roomId, false);
        }
    }
}
