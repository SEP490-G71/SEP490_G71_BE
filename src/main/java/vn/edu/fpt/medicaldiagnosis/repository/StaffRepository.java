package vn.edu.fpt.medicaldiagnosis.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository  extends JpaRepository<Staff, UUID> {
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);

    boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String email, UUID id);


    List<Staff> findAllByDeletedAtIsNull();

    Optional<Staff> findByIdAndDeletedAtIsNull(UUID id);

    Page<Staff> findAll(Specification<Staff> spec, Pageable pageable);
}
