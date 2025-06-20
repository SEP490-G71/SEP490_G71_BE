package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQueueJob {

    private final DailyQueueService dailyQueueService;

    /**
     * Tạo mới một hàng đợi mỗi ngày lúc 7:00 sáng
     */
    @Scheduled(cron = "0 0 7 * * *") // 7h sáng mỗi ngày
    public void createDailyQueueAt7AM() {
        LocalDateTime now = LocalDateTime.now().with(LocalTime.of(7, 0));
        log.info("Bắt đầu tạo daily queue cho ngày {}", now.toLocalDate());

        DailyQueueRequest request = DailyQueueRequest.builder()
                .queueDate(now)
                .status("ACTIVE")
                .build();

        dailyQueueService.createDailyQueue(request);

        log.info("Tạo daily queue thành công cho ngày {}", now.toLocalDate());
    }

    /**
     * Đóng hàng đợi mỗi ngày lúc 18:00 (6 giờ chiều)
     */
    @Scheduled(cron = "0 0 18 * * *") // 18h mỗi ngày
    public void closeDailyQueueAt6PM() {
        log.info("Bắt đầu đóng daily queue lúc 18h");
        dailyQueueService.closeTodayQueue();
    }

}
