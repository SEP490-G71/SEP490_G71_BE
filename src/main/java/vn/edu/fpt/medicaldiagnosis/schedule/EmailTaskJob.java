package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;

import java.util.List;

@Slf4j
@Component
public class EmailTaskJob {

    @Autowired
    private EmailTaskRepository emailTaskRepository;

    @Autowired
    private EmailService emailService;

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 10000)
    public void processEmails() {
        List<EmailTask> tasks = emailTaskRepository.findTop10ByStatusOrderByCreatedAtAsc(Status.PENDING);

        for (EmailTask task : tasks) {
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
}
