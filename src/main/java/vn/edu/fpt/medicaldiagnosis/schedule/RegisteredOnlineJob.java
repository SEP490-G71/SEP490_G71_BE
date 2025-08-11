package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineStatusRequest;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.RegisteredOnlineService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisteredOnlineJob {

    private final TenantService tenantService;
    private final RegisteredOnlineService registeredOnlineService;

    @Scheduled(cron = "0 0 19 * * *") // 19h sáng mỗi ngày
    public void expireActiveRegistrationsToday() {
        LocalDate today = LocalDate.now();
        List<Tenant> tenants = tenantService.getAllTenantsActive();

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getCode());
                log.info("[{}] Expire RegisteredOnline ACTIVE ngày {}", tenant.getCode(), today);

                registeredOnlineService.getActiveRegisteredToday()
                        .forEach(r -> {
                            RegisteredOnlineStatusRequest req = new RegisteredOnlineStatusRequest();
                            req.setStatus(Status.INACTIVE);
                            registeredOnlineService.updateStatus(r.getId(), req);
                        });

            } catch (Exception e) {
                log.error("[{}] Lỗi expire RegisteredOnline: {}", tenant.getCode(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

}
