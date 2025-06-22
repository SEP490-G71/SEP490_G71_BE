package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = "SELECT COALESCE(MAX(queue_order), 0) FROM queue_patients WHERE department_id = :departmentId AND queue_id = :queueId FOR UPDATE", nativeQuery = true)
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

    @Query(value = """
        SELECT * FROM queue_patients 
        WHERE queue_id = :queueId
          AND department_id = :departmentId
          AND status IN (:statuses)
        ORDER BY queue_order ASC
    """, nativeQuery = true)
    List<QueuePatients> findAssigned(String queueId, String departmentId, List<String> statuses);


    @Modifying
    @Query(value = """
        UPDATE queue_patients 
        SET department_id = :roomId, queue_order = :queueOrder 
        WHERE id = :id AND department_id IS NULL
    """, nativeQuery = true)
    int tryAssignRoom(
            @Param("id") String patientId,
            @Param("roomId") String roomId,
            @Param("queueOrder") long queueOrder
    );


}
