package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.util.List;

public interface EmailTaskRepository extends JpaRepository<EmailTask, String> {
    List<EmailTask> findTop10ByStatusOrderByCreatedAtAsc(Status status);
}
