package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.DailyQueueRepository;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQueueJob {

    private final DailyQueueService dailyQueueService;
    private final TenantService tenantService;
    private final DailyQueueRepository dailyQueueRepository;

    @Scheduled(cron = "0 0 7 * * *") // 7h sáng mỗi ngày
    public void createDailyQueueAt7AM() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();
        LocalDate today = LocalDate.now();
        LocalDateTime queueDateTime = today.atTime(7, 0);

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getCode());
                log.info("[{}] Bắt đầu xử lý hàng đợi ngày {}", tenant.getCode(), today);

                Optional<DailyQueue> existingQueueOpt = dailyQueueRepository.findByQueueDateAndDeletedAtIsNull(queueDateTime);

                if (existingQueueOpt.isPresent()) {
                    DailyQueue existingQueue = existingQueueOpt.get();
                    existingQueue.setStatus(Status.ACTIVE.name());
                    dailyQueueRepository.save(existingQueue);
                    log.info("[{}] Cập nhật hàng đợi ngày {} thành ACTIVE", tenant.getCode(), today);
                } else {
                    DailyQueueRequest request = DailyQueueRequest.builder()
                            .queueDate(queueDateTime)
                            .status(Status.ACTIVE.name())
                            .build();
                    dailyQueueService.createDailyQueue(request);
                    log.info("[{}] Tạo mới hàng đợi ngày {} thành công", tenant.getCode(), today);
                }
            } catch (Exception e) {
                log.error("[{}] Lỗi khi xử lý hàng đợi ngày {}: {}", tenant.getCode(), today, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 0 18 * * *") // 18h mỗi ngày
    public void closeDailyQueueAt6PM() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getCode());
                log.info("[{}] Bắt đầu đóng daily queue lúc 18h", tenant.getCode());

                dailyQueueService.closeTodayQueue();

                log.info("[{}] Đóng daily queue thành công", tenant.getCode());
            } catch (Exception e) {
                log.error("[{}] Lỗi khi đóng daily queue: {}", tenant.getCode(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
