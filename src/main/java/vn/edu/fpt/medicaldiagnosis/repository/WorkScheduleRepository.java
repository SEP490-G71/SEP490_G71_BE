package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;

import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String>, JpaSpecificationExecutor<WorkSchedule> {
    Optional<WorkSchedule> findByIdAndDeletedAtIsNull(String scheduleId);

    List<WorkSchedule> findAllByStaffIdAndDeletedAtIsNullOrderByShiftDateAsc(String staffId);

    Page<WorkSchedule> findAll(Specification<WorkSchedule> spec, Pageable pageable);

    List<WorkSchedule> findAllByStaffIdAndShiftAndDeletedAtIsNull(String staffId, Shift shift);
}
