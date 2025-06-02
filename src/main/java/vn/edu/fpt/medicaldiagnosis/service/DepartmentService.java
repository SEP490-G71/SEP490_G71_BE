package vn.edu.fpt.medicaldiagnosis.service;



import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest departmentCreateRequest);
    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(UUID id);

    void deleteDepartment(UUID id);

    DepartmentResponse updateDepartment(UUID id, DepartmentUpdateRequest departmentUpdateRequest);

    Page<DepartmentResponse> getDepartmentsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
