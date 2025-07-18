package vn.edu.fpt.medicaldiagnosis.service;



import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.AssignStaffRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest departmentCreateRequest);
    List<DepartmentResponse> getAllDepartments();

    DepartmentDetailResponse getDepartmentById(String id);

    void deleteDepartment(String id);

    DepartmentResponse updateDepartment(String id, DepartmentUpdateRequest departmentUpdateRequest);

    Page<DepartmentResponse> getDepartmentsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    DepartmentDetailResponse assignStaffsToDepartment(String departmentId, AssignStaffRequest request);

    DepartmentResponse getMyDepartment(String username);
}
