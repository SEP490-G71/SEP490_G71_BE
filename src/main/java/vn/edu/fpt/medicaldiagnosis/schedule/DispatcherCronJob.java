package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.thread.dispatcher.Dispatcher;
import vn.edu.fpt.medicaldiagnosis.thread.manager.RoomManager;
import vn.edu.fpt.medicaldiagnosis.thread.worker.RoomWorker;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class DispatcherCronJob {

    private static boolean started = false;

    @Scheduled(fixedDelay = 2000) // Mỗi ngày lúc 8:00 sáng
    public void runDispatchSystem() {
        if (started) {
            log.info("Hệ thống xếp phòng đã được chạy, bỏ qua.");
            return;
        }

        log.info("⏰ CRON: Khởi động hệ thống phân phối bệnh nhân...");

        final int TOTAL_ROOMS = 5;
        final int MAX_PATIENTS = 20;

        RoomManager roomManager = new RoomManager(TOTAL_ROOMS);
        Queue<Patient> cacheQueue = new LinkedList<>();
        Set<String> seenIds = new HashSet<>();
        Object lock = new Object();

        // Tạo danh sách bệnh nhân mẫu
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

        // Khởi động Dispatcher
        new Dispatcher(mockDatabase, cacheQueue, seenIds, lock, roomManager).start();

        // Khởi động các RoomWorker
        for (int i = 0; i < TOTAL_ROOMS; i++) {
            new RoomWorker(i, cacheQueue, lock, roomManager).start();
        }

        started = true;
    }
}
