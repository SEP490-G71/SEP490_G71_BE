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
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleReportResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.WorkScheduleReportResponseInterface;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.WorkStatus;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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

    boolean existsByStaffIdAndShiftIdAndShiftDate(String id, String id1, LocalDate date);

    List<WorkSchedule> findAllByStaffIdAndShiftDateInAndShiftIdIn(String id, List<LocalDate> allDates, List<String> shiftIds);

    @Query("""
    SELECT ws FROM WorkSchedule ws
    WHERE ws.staff.id = :staffId
      AND FUNCTION('TIMESTAMP', ws.shiftDate, ws.shift.startTime) >= :from
      AND FUNCTION('TIMESTAMP', ws.shiftDate, ws.shift.endTime) <= :to
      AND ws.deletedAt IS NULL
""")
    List<WorkSchedule> findByStaffIdAndDateTimeRange(@Param("staffId") String staffId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);


    List<WorkSchedule> findAllByStaff_IdAndShiftDate(String staffId, LocalDate today);

    List<WorkSchedule> findAllByShift_IdAndShiftDateAndCheckInTimeIsNullAndStatusNot(String shiftId, LocalDate today, WorkStatus workStatus);

    @Query(value = """
    SELECT
      s.id AS staffId,
      s.full_name AS staffName,
      s.staff_code AS staffCode,
      COUNT(ws.id) AS totalShifts,
      SUM(CASE WHEN ws.status = 'ATTENDED' THEN 1 ELSE 0 END) AS attendedShifts,
      SUM(CASE WHEN ws.status = 'ABSENT' THEN 1 ELSE 0 END) AS leaveShifts,
      ROUND(SUM(CASE WHEN ws.status = 'ATTENDED' THEN 1 ELSE 0 END) * 100.0 / COUNT(ws.id), 2) AS attendanceRate,
      ROUND(SUM(CASE WHEN ws.status = 'ABSENT' THEN 1 ELSE 0 END) * 100.0 / COUNT(ws.id), 2) AS leaveRate
    FROM work_schedules ws
    JOIN staffs s ON ws.staff_id = s.id
    WHERE MONTH(ws.shift_date) = MONTH(CURDATE())
      AND YEAR(ws.shift_date) = YEAR(CURDATE())
    GROUP BY s.id, s.full_name, s.staff_code
    ORDER BY totalShifts DESC
    LIMIT 5
""", nativeQuery = true)
    List<WorkScheduleReportResponseInterface> getWorkScheduleReportThisMonth();

    long countByStaff_Department_IdAndShiftDateAndStatusIn(
            String departmentId, LocalDate date, Collection<WorkStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update WorkSchedule ws
           set ws.status = :absent
         where ws.shiftDate = :d
           and ws.deletedAt is null
           and ws.status = :scheduled
    """)
    int markAllScheduledAsAbsent(@Param("d") LocalDate date,
                                 @Param("scheduled") WorkStatus scheduled,
                                 @Param("absent") WorkStatus absent);

    List<WorkSchedule> findAllByStaff_IdAndShiftDateBetweenAndDeletedAtIsNull(String staffId, LocalDate yesterday, LocalDate today);
}
