package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.common.RoleMapperHelper;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;

import java.util.Optional;

public interface RegisteredOnlineRepository extends JpaRepository<RegisteredOnline, String> {
    Optional<RegisteredOnline> findByIdAndDeletedAtIsNull(String id);

    Page<RegisteredOnline> findAllByDeletedAtIsNull(Pageable pageable);

    Page<RegisteredOnline> findAll(Specification<RegisteredOnline> spec, Pageable pageable);

    Optional<RegisteredOnline> findByEmailOrPhoneNumberAndDeletedAtIsNull(String email, String phoneNumber);
}
