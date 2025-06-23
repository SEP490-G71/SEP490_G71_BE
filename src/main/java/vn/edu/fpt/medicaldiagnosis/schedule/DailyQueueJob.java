package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQueueJob {

    private final DailyQueueService dailyQueueService;
    private final TenantService tenantService;

    @Scheduled(cron = "0 0 7 * * *") // 7h sáng mỗi ngày
    public void createDailyQueueAt7AM() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();
        LocalDateTime now = LocalDateTime.now().with(LocalTime.of(7, 0));

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getCode());
                log.info("[{}] Bắt đầu tạo daily queue cho ngày {}", tenant.getCode(), now.toLocalDate());

                DailyQueueRequest request = DailyQueueRequest.builder()
                        .queueDate(now)
                        .status("ACTIVE")
                        .build();

                dailyQueueService.createDailyQueue(request);

                log.info("[{}] Tạo daily queue thành công cho ngày {}", tenant.getCode(), now.toLocalDate());
            } catch (Exception e) {
                log.error("[{}] Lỗi khi tạo daily queue: {}", tenant.getCode(), e.getMessage(), e);
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
