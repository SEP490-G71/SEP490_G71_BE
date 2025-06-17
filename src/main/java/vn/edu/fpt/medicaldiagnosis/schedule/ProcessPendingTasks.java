package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.enums.Action;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.DbTaskRepository;
import vn.edu.fpt.medicaldiagnosis.service.DbTaskService;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProcessPendingTasks {

    private final DbTaskRepository dbTaskRepository;

    private final DbTaskService dbTaskService;

    @Scheduled(fixedDelay = 1000)
    public void processPendingTasks() {
        List<DbTask> tasks = dbTaskService.findByStatus(Status.PENDING);
        for (DbTask task : tasks) {
            try {
                task.setStatus(Status.IN_PROGRESS);
                dbTaskRepository.save(task);

                if (task.getAction() == Action.CREATE) {
                    dbTaskService.createDatabase(task.getTenantCode());
                } else {
                    dbTaskService.dropDatabase(task.getTenantCode());
                }

                task.setStatus(Status.DONE);
            } catch (Exception e) {
                task.setStatus(Status.FAILED);
                task.setMessage(e.getMessage());
                log.error("Task {} failed: {}", task.getId(), e.getMessage());
            } finally {
                dbTaskRepository.save(task);
            }
        }
    }
}
