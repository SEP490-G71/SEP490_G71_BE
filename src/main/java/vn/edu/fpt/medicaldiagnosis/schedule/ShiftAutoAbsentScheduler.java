package vn.edu.fpt.medicaldiagnosis.schedule;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Setting;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;
import vn.edu.fpt.medicaldiagnosis.repository.SettingRepository;
import vn.edu.fpt.medicaldiagnosis.repository.ShiftRepository;
import vn.edu.fpt.medicaldiagnosis.repository.WorkScheduleRepository;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;
import vn.edu.fpt.medicaldiagnosis.service.ShiftService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShiftAutoAbsentScheduler {

    private final TaskScheduler taskScheduler;
    private final SettingService settingService;
    private final ShiftRepository shiftRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final TenantService tenantService;

    @Scheduled(cron = "0 0 0 * * *") // chạy mỗi ngày lúc 00:00
    public void scheduleShiftCheckTasksForAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();
        for (Tenant tenant : tenants) {
            try {
                scheduleShiftCheckTasksForTenant(tenant.getCode());
            } catch (Exception e) {
                log.error("[{}] ❌ Lỗi khi lên lịch đánh vắng: {}", tenant.getCode(), e.getMessage(), e);
            }
        }
    }

    public void scheduleShiftCheckTasksForTenant(String tenantCode) {
        TenantContext.setTenantId(tenantCode);
        try {
            List<Shift> shifts = shiftRepository.findAll();
            SettingResponse setting = settingService.getSetting();
            int lateMinutes = setting.getLatestCheckInMinutes();

            for (Shift shift : shifts) {
                LocalDateTime runAt = LocalDateTime.of(LocalDate.now(), shift.getStartTime().plusMinutes(lateMinutes));
                if (runAt.isBefore(LocalDateTime.now())) continue;

                taskScheduler.schedule(() -> {
                    try {
                        TenantContext.setTenantId(tenantCode); // đảm bảo thread có tenant context
                        runShiftCheck(shift.getId(), tenantCode);
                    } finally {
                        TenantContext.clear();
                    }
                }, Timestamp.valueOf(runAt));

                log.info("[{}] ⏰ Đã lên lịch đánh vắng cho ca '{}' lúc {}", tenantCode, shift.getName(), runAt);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void runShiftCheck(String shiftId, String tenantCode) {
        LocalDate today = LocalDate.now();
        log.info("[{}] ▶️ Quét điểm danh muộn cho ca {} vào {}", tenantCode, shiftId, LocalTime.now());

        List<WorkSchedule> schedules = workScheduleRepository
                .findAllByShift_IdAndShiftDateAndCheckInTimeIsNullAndStatusNot(
                        shiftId, today, WorkStatus.ATTENDED);

        for (WorkSchedule ws : schedules) {
            ws.setStatus(WorkStatus.ABSENT);
            workScheduleRepository.save(ws);
            log.info("[{}] 🚫 Đánh vắng nhân viên {} trong ca {}", tenantCode, ws.getStaff().getId(), ws.getShift().getName());
        }
    }
}
