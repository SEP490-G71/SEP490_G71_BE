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
            "AND checkin_time >= CURDATE()", nativeQuery = true)
    List<QueuePatients> findAllByStatusAndDeletedAtIsNull(String status);


    @Query(value = "SELECT * FROM queue_patients WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<QueuePatients> findByIdForUpdate(@Param("id") String id);
}
