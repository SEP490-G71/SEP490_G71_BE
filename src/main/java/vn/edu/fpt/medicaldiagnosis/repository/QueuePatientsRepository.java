package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueuePatientsRepository extends JpaRepository<QueuePatients, String> {
    Optional<QueuePatients> findByIdAndDeletedAtIsNull(String id);

    List<QueuePatients> findAllByDeletedAtIsNull();

    @Query(value = "SELECT * FROM queue_patients " +
            "WHERE status = :status " +
            "AND deleted_at IS NULL " +
            "AND queue_id = :queueId",
            nativeQuery = true)
    List<QueuePatients> findAllByStatusAndQueueId(@Param("status") String status, @Param("queueId") String queueId);


    @Query(value = "SELECT * FROM queue_patients WHERE queue_id = :queueId AND deleted_at IS NULL ORDER BY queue_order DESC LIMIT 1 FOR UPDATE", nativeQuery = true)
    Optional<QueuePatients> findLastByQueueIdForUpdate(@Param("queueId") String queueId);

    @Query(value = "SELECT COALESCE(MAX(queue_order), 0) FROM queue_patients WHERE department_id = :departmentId AND queue_id = :queueId", nativeQuery = true)
    Long findMaxQueueOrderByRoom(@Param("departmentId") String departmentId, @Param("queueId") String queueId);

    @Query(value = """
            SELECT * FROM queue_patients 
            WHERE status = 'WAITING' 
              AND queue_id = :queueId 
              AND (department_id IS NULL OR queue_order IS NULL) 
            ORDER BY checkin_time ASC, id ASC
            LIMIT :limit
        """, nativeQuery = true)
    List<QueuePatients> findTopUnassignedWaiting(@Param("queueId") String queueId, @Param("limit") int limit);
}
