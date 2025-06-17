package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQueueRepository extends JpaRepository<DailyQueue, String> {
    Optional<DailyQueue> findByIdAndDeletedAtIsNull(String id);
    List<DailyQueue> findAllByDeletedAtIsNull();

    @Query(value = "SELECT * FROM daily_queues WHERE status = :status AND deleted_at IS NULL ORDER BY queue_date DESC LIMIT 1", nativeQuery = true)
    Optional<DailyQueue> findFirstByStatusOrderByQueueDateDesc(@Param("status") String status);

    @Query(value = "SELECT * FROM daily_queues WHERE status = 'ACTIVE' AND deleted_at IS NULL ORDER BY queue_date DESC LIMIT 1", nativeQuery = true)
    Optional<DailyQueue> findActiveQueueForToday();

}
