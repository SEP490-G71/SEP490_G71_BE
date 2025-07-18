package vn.edu.fpt.medicaldiagnosis.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String>, JpaSpecificationExecutor<WorkSchedule> {
    Optional<WorkSchedule> findByIdAndDeletedAtIsNull(String scheduleId);

    List<WorkSchedule> findAllByStaffIdAndDeletedAtIsNullOrderByShiftDateAsc(String staffId);

    Page<WorkSchedule> findAll(Specification<WorkSchedule> spec, Pageable pageable);


    List<WorkSchedule> findAllByStaffIdAndShiftDateBetweenAndDeletedAtIsNull(String staffId, LocalDate startDate, LocalDate endDate);

    @Query("""
    SELECT ws FROM WorkSchedule ws
    WHERE ws.staff.id = :staffId
      AND ws.deletedAt IS NULL
      AND ws.status <> 'ATTENDED'
      AND ws.shiftDate >= :now
""")
    List<WorkSchedule> findActiveFutureSchedulesByStaffId(@Param("staffId") String staffId, @Param("now") LocalDate now);

    @Modifying
    @Transactional
    @Query("DELETE FROM WorkSchedule ws WHERE ws.id = :id AND ws.deletedAt IS NULL")
    void deleteByIdHard(String id);

    @Modifying
    @Transactional
    @Query("DELETE FROM WorkSchedule ws WHERE ws.staff.id = :staffId AND ws.shiftDate >= :today AND ws.status <> 'ATTENDED'")
    void deleteFutureUnattendedByStaffId(String staffId, LocalDate today);

    Optional<WorkSchedule> findByStaffIdAndShiftDateAndShiftAndDeletedAtIsNull(String id, LocalDate date, Shift shift);

    boolean existsByStaffIdAndShiftDateAndShiftAndIdNot(String id, LocalDate shiftDate, Shift shift, String id1);
}
