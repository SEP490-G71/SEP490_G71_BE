package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;

import java.time.LocalTime;

public interface ShiftRepository extends JpaRepository<Shift, String> {
    boolean existsByName(String name);


    @Query("SELECT COUNT(s) > 0 FROM Shift s WHERE " +
            ":startTime < s.endTime AND :endTime > s.startTime")
    boolean existsByOverlappingTime(@Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(s) > 0 FROM Shift s " +
            "WHERE :startTime < s.endTime AND :endTime > s.startTime AND s.id <> :id")
    boolean existsByOverlappingTimeExcludeId(@Param("startTime") LocalTime startTime,
                                             @Param("endTime") LocalTime endTime,
                                             @Param("id") String id);

    Page<Shift> findAll(Specification<Shift> spec, Pageable pageable);
}
