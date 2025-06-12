package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQueueRepository extends JpaRepository<DailyQueue, String> {
    Optional<DailyQueue> findByIdAndDeletedAtIsNull(String id);
    List<DailyQueue> findAllByDeletedAtIsNull();
}
