package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EodAbsentJob {
    private final TenantService tenantService;
    private final SettingService settingService; // để lấy timezone/setting mỗi tenant (nếu có)
    private final WorkScheduleRepository workScheduleRepository;

    // Chạy 00:05 mỗi ngày theo server; bên trong sẽ tính ngày theo timezone của tenant
    @Scheduled(cron = "0 5 0 * * *")
    public void runForAllTenants() {
        for (Tenant t : tenantService.getAllTenantsActive()) {
            try {
                TenantContext.setTenantId(t.getCode());
                runForTenant(t.getCode());
            } catch (Exception e) {
                log.error("[{}] ❌ EOD absent job error: {}", t.getCode(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    protected void runForTenant(String tenantCode) {
        String tz =  "Asia/Ho_Chi_Minh";
        ZoneId zoneId = ZoneId.of(tz);

        // Hôm qua theo timezone của tenant
        LocalDate targetDate = ZonedDateTime.now(zoneId).toLocalDate().minusDays(1);

        int updated = workScheduleRepository.markAllScheduledAsAbsent(
                targetDate, WorkStatus.SCHEDULED, WorkStatus.ABSENT);

        log.info("[{}] ✅ EOD absent: {} records updated for date {}", tenantCode, updated, targetDate);
    }
}
