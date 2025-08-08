package vn.edu.fpt.medicaldiagnosis.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository  extends JpaRepository<Staff, String> {
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, String id);

    boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String email, String id);

    Optional<Staff> findByAccountId(String accountId);

    List<Staff> findAllByDeletedAtIsNull();

    Optional<Staff> findByIdAndDeletedAtIsNull(String id);

    Page<Staff> findAll(Specification<Staff> spec, Pageable pageable);


    List<Staff> findByFullNameContainingIgnoreCaseOrStaffCodeContainingIgnoreCase(String keyword, String keyword1);

    List<Staff> findByDepartmentId(String departmentId);

    List<Staff> findByDepartmentIsNullAndDeletedAtIsNull();

    List<Staff> findByDepartmentIsNullAndDeletedAtIsNullAndFullNameContainingIgnoreCaseOrDepartmentIsNullAndDeletedAtIsNullAndStaffCodeContainingIgnoreCase(String keyword, String keyword1);

    Optional<Staff> findByAccountIdAndDeletedAtIsNull(String id);

    @Query("SELECT s FROM Staff s " +
            "JOIN Account a ON a.id = s.accountId " +
            "JOIN a.roles r " +
            "WHERE s.department IS NULL AND r.name = :roleName " +
            "AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Staff> findUnassignedStaffByRoleAndKeyword(@Param("roleName") String roleName,
                                                    @Param("keyword") String keyword);

}
