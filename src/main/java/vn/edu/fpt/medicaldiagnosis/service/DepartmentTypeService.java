package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentTypeRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentTypeResponse;

import java.util.List;
import java.util.Map;

public interface DepartmentTypeService {
    DepartmentTypeResponse create(DepartmentTypeRequest request);
    DepartmentTypeResponse update(String id, DepartmentTypeRequest request);
    void delete(String id);
    DepartmentTypeResponse getById(String id);
    List<DepartmentTypeResponse> getAll();

    Page<DepartmentTypeResponse> getPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
