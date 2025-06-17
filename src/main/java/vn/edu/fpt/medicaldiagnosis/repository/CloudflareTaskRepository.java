package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.CloudflareTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.util.List;

public interface CloudflareTaskRepository extends JpaRepository<CloudflareTask, String> {
    List<CloudflareTask> findTop10ByStatusOrderByCreatedAtAsc(Status status);
}
