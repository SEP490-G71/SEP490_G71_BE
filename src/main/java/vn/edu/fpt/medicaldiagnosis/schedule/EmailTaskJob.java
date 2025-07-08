package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.util.List;

@Slf4j
@Component
public class EmailTaskJob {

    @Autowired
    private EmailTaskRepository emailTaskRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TenantService tenantService;

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 10000)
    public void processEmails() {
//        log.info("⏰ EmailTaskJob is scanning for pending emails...");
        List<EmailTask> tasks = emailTaskRepository.findTop10ByStatusOrderByCreatedAtAsc(Status.PENDING);
//        log.info("Found {} pending emails", tasks.size());
        for (EmailTask task : tasks) {
            log.info("Processing email task: {}", task.getId());
            try {
                emailService.sendSimpleMail(
                        task.getEmailTo(),
                        task.getSubject(),
                        task.getContent()
                );

                task.setStatus(Status.DONE);
                log.info("Đã gửi email tới {}", task.getEmailTo());
            } catch (Exception e) {
                int retry = task.getRetryCount() + 1;
                task.setRetryCount(retry);

                if (retry >= MAX_RETRIES) {
                    task.setStatus(Status.FAILED);
                    log.error("Gửi email thất bại vĩnh viễn tới {} sau {} lần thử: {}", task.getEmailTo(), retry, e.getMessage());
                } else {
                    task.setStatus(Status.PENDING);
                    log.warn("Lỗi gửi email lần {} tới {}: {}. Sẽ thử lại.", retry, task.getEmailTo(), e.getMessage());
                }
            }

            emailTaskRepository.save(task);
        }
    }

    @Scheduled(fixedDelay = 15000) // chạy mỗi 15s để test
    public void processEmailsForAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();
        for (Tenant tenant : tenants) {
            try {
                processEmailsForTenant(tenant.getCode());
            } catch (Exception e) {
                log.error("[{}] Lỗi khi xử lý email: {}", tenant.getCode(), e.getMessage(), e);
            }
        }
    }

    public void processEmailsForTenant(String tenantCode) {
        TenantContext.setTenantId(tenantCode);
//        log.info("[{}] Quét email PENDING...", tenantCode);

        List<EmailTask> tasks = emailTaskRepository.findTop10ByStatusOrderByCreatedAtAsc(Status.PENDING);
//        log.info("[{}] Tìm thấy {} email đang chờ gửi", tenantCode, tasks.size());

        for (EmailTask task : tasks) {
//            log.info("[{}] Đang xử lý email task: {}", tenantCode, task.getId());
            try {
                emailService.sendSimpleMail(task.getEmailTo(), task.getSubject(), task.getContent());
                task.setStatus(Status.DONE);
                log.info("[{}] ✅ Đã gửi email tới {}", tenantCode, task.getEmailTo());
            } catch (Exception e) {
                int retry = task.getRetryCount() + 1;
                task.setRetryCount(retry);

                if (retry >= MAX_RETRIES) {
                    task.setStatus(Status.FAILED);
                    log.error("[{}] ❌ Gửi thất bại tới {} sau {} lần thử: {}", tenantCode, task.getEmailTo(), retry, e.getMessage());
                } else {
                    task.setStatus(Status.PENDING);
                    log.warn("[{}] ⚠️ Lỗi gửi lần {} tới {}: {}. Sẽ thử lại.", tenantCode, retry, task.getEmailTo(), e.getMessage());
                }
            }
            emailTaskRepository.save(task);
        }

        TenantContext.clear(); // Đảm bảo reset sau mỗi tenant
    }

}
