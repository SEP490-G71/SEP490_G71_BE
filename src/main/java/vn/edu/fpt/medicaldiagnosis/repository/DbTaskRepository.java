package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.util.List;

public interface DbTaskRepository extends JpaRepository<DbTask, String> {
    List<DbTask> findByStatus(Status status);
}
