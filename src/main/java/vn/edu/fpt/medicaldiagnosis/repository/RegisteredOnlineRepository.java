package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.common.RoleMapperHelper;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegisteredOnlineRepository extends JpaRepository<RegisteredOnline, String> {
    Optional<RegisteredOnline> findByIdAndDeletedAtIsNull(String id);

    Page<RegisteredOnline> findAllByDeletedAtIsNull(Pageable pageable);

    Page<RegisteredOnline> findAll(Specification<RegisteredOnline> spec, Pageable pageable);

    Optional<RegisteredOnline> findByEmailOrPhoneNumberAndDeletedAtIsNull(String email, String phoneNumber);

    @Query("""
    SELECT r FROM RegisteredOnline r
     WHERE r.status = :status
       AND r.deletedAt IS NULL
       AND DATE(r.registeredAt) = :date
    """)
    List<RegisteredOnline> findByStatusAndRegisteredDate(
            @Param("status") Status status,
            @Param("date") LocalDate date
    );

}
