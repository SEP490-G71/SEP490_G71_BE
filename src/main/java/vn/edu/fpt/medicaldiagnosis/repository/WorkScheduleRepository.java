package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;

import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String> {
    Optional<WorkSchedule> findByIdAndDeletedAtIsNull(String scheduleId);

    List<WorkSchedule> findAllByStaffIdAndDeletedAtIsNullOrderByShiftDateAsc(String staffId);
}
