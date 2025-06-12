package vn.edu.fpt.medicaldiagnosis.thread.dispatcher;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.time.LocalDate;
import java.util.*;

public class Dispatcher extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private final List<Patient> mockDatabase;
    private final Queue<Patient> cacheQueue;
    private final Set<String> seenIds;
    private final Object lock;
    private final RoomManager roomManager;

    public Dispatcher(List<Patient> mockDatabase, Queue<Patient> cacheQueue, Set<String> seenIds,
                      Object lock, RoomManager roomManager) {
        this.mockDatabase = mockDatabase;
        this.cacheQueue = cacheQueue;
        this.seenIds = seenIds;
        this.lock = lock;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        int offset = 0;
        while (offset < mockDatabase.size()) {
            synchronized (lock) {
                int idleRooms = roomManager.getIdleRoomCount();
                int remaining = mockDatabase.size() - offset;
                int batch = Math.min(idleRooms, remaining);

                // Chỉ đẩy batch bệnh nhân khi có phòng rảnh
                for (int i = 0; i < batch; i++) {
                    Patient p = mockDatabase.get(offset++);
                    if (seenIds.contains(p.getId())) continue;

                    cacheQueue.offer(p);
                    seenIds.add(p.getId());

                    log.info("Đã nạp bệnh nhân vào hàng đợi: {}", p.getFullName());
                }

                lock.notifyAll(); // Báo cho phòng biết có bệnh nhân
            }

            try {
                Thread.sleep(300); // kiểm tra định kỳ
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Entry point: Tạo dữ liệu, khởi chạy Dispatcher và RoomWorker
    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        log.info("Password: {}", passwordEncoder.encode("123456"));

        final int TOTAL_ROOMS = 5;
        final int MAX_PATIENTS = 20;

        RoomManager roomManager = new RoomManager(TOTAL_ROOMS);
        Queue<Patient> cacheQueue = new LinkedList<>();
        Set<String> seenIds = new HashSet<>();
        Object lock = new Object();

        List<Patient> mockDatabase = new ArrayList<>();
        for (int i = 1; i <= MAX_PATIENTS; i++) {
            mockDatabase.add(new Patient(
                    "Nguyen", "Van", "A" + i,
                    LocalDate.of(2000, 1, i % 28 + 1),
                    i % 2 == 0 ? Gender.MALE : Gender.FEMALE,
                    "09" + i + "123456",
                    "bna" + i + "@mail.com",
                    UUID.randomUUID().toString()
            ));
        }

        // Khởi chạy Dispatcher
        new Dispatcher(mockDatabase, cacheQueue, seenIds, lock, roomManager).start();

        // Khởi chạy các phòng khám
        for (int i = 0; i < TOTAL_ROOMS; i++) {
            new RoomWorker(i, cacheQueue, lock, roomManager).start();
        }
    }
}
