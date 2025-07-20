package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueuePatientsRepository extends JpaRepository<QueuePatients, String> {

    Optional<QueuePatients> findByIdAndDeletedAtIsNull(String id);

    @Query("""
        SELECT qp FROM QueuePatients qp
        LEFT JOIN FETCH qp.specialization
        WHERE qp.queueId = :queueId
        ORDER BY qp.roomNumber ASC, qp.queueOrder ASC, qp.isPriority DESC
    """)
    List<QueuePatients> findAllByQueueId(@Param("queueId") String queueId);

    @Query(value = """
        SELECT * FROM queue_patients
        WHERE deleted_at IS NULL
          AND status = :status
          AND queue_id = :queueId
    """, nativeQuery = true)
    List<QueuePatients> findAllByStatusAndQueueId(@Param("status") String status, @Param("queueId") String queueId);

    @Query(value = """
        SELECT COALESCE(MAX(queue_order), 0)
        FROM queue_patients
        WHERE room_number = :roomNumber
          AND queue_id = :queueId
        FOR UPDATE
    """, nativeQuery = true)
    Long findMaxQueueOrderByRoom(@Param("roomNumber") String roomNumber, @Param("queueId") String queueId);

    @Query(value = """
        SELECT * FROM queue_patients 
        WHERE deleted_at IS NULL
          AND status = 'WAITING'
          AND is_priority = false
          AND queue_id = :queueId
        ORDER BY registered_time ASC, created_at ASC, id ASC
        LIMIT :limit
    """, nativeQuery = true)
    List<QueuePatients> findTopUnassignedWaiting(@Param("queueId") String queueId, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM queue_patients 
        WHERE deleted_at IS NULL
          AND status = 'WAITING'
          AND is_priority = true
          AND queue_id = :queueId
        ORDER BY registered_time ASC, created_at ASC, id ASC
        LIMIT :limit
    """, nativeQuery = true)
    List<QueuePatients> findTopPriorityWaiting(@Param("queueId") String queueId, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM queue_patients 
        WHERE deleted_at IS NULL
          AND queue_id = :queueId
          AND room_number = :roomNumber
          AND status IN (:statuses)
        ORDER BY queue_order ASC
    """, nativeQuery = true)
    List<QueuePatients> findAssigned(
            @Param("queueId") String queueId,
            @Param("roomNumber") String roomNumber,
            @Param("statuses") List<String> statuses
    );

    @Modifying
    @Query(value = """
    UPDATE queue_patients
    SET room_number = :roomNumber,
        queue_order = :queueOrder,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = :patientId
      AND deleted_at IS NULL
""", nativeQuery = true)
    int tryAssignRoom(@Param("patientId") String patientId,
                      @Param("roomNumber") String roomNumber,
                      @Param("queueOrder") long queueOrder);

    @Query(value = """
        SELECT COUNT(*) 
        FROM queue_patients
        WHERE deleted_at IS NULL
          AND queue_id = :queueId
          AND room_number = :roomNumber
          AND queue_order = :queueOrder
    """, nativeQuery = true)
    int countQueueOrderConflict(
            @Param("queueId") String queueId,
            @Param("roomNumber") String roomNumber,
            @Param("queueOrder") Long queueOrder
    );

    @Query(value = """
        SELECT * FROM queue_patients
        WHERE deleted_at IS NULL
          AND queue_id = :queueId
          AND room_number = :roomNumber
        ORDER BY 
          is_priority DESC,
          queue_order ASC,
    """, nativeQuery = true)
    List<QueuePatients> findAllByQueueIdAndRoomNumber(
            @Param("queueId") String queueId,
            @Param("roomNumber") String roomNumber
    );

    @Query(value = """
        SELECT COUNT(*) FROM queue_patients
        WHERE deleted_at IS NULL
          AND queue_id = :queueId
          AND patient_id = :patientId
          AND status IN ('WAITING', 'IN_PROGRESS')
    """, nativeQuery = true)
    int countActiveVisits(
            @Param("queueId") String queueId,
            @Param("patientId") String patientId
    );

    @EntityGraph(attributePaths = {"specialization"})
    Page<QueuePatients> findAll(Specification<QueuePatients> spec, Pageable pageable);
}
