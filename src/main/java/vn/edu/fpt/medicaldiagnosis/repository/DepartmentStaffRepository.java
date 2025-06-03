package vn.edu.fpt.medicaldiagnosis.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentStaff;

import java.util.List;
import java.util.UUID;

public interface DepartmentStaffRepository extends JpaRepository<DepartmentStaff, UUID> {
    List<DepartmentStaff> findByDepartmentId(UUID departmentId);
    void deleteByDepartmentId(UUID departmentId);
}
