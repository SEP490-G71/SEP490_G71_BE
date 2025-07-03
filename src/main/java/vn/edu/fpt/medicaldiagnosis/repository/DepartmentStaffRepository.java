package vn.edu.fpt.medicaldiagnosis.repository;



import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentStaff;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

import java.util.List;
import java.util.Optional;

public interface DepartmentStaffRepository extends JpaRepository<DepartmentStaff, String> {
    List<DepartmentStaff> findByDepartmentId(String departmentId);
    void deleteByDepartmentId(String departmentId);
    @Modifying
    @Transactional
    @Query("DELETE FROM DepartmentStaff ds WHERE ds.staff.id = :staffId")
    void deleteByStaffId(@Param("staffId") String staffId);

    Optional<DepartmentStaff> findByStaffId(String id);
}
