package vn.edu.fpt.medicaldiagnosis.service;


import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.util.List;
import java.util.Map;


public interface StaffService {
    StaffResponse createStaff(StaffCreateRequest staffCreateRequest);

    List<StaffResponse> getAllStaffs();

    StaffResponse getStaffById(String id);

    void deleteStaff(String id);

    StaffResponse updateStaff(String id, StaffUpdateRequest staffCreateRequest);

    Page<StaffResponse> getStaffsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    List<StaffResponse> getStaffNotAssignedToAnyDepartment(String keyword, DepartmentType departmentType);

    List<StaffResponse> searchByNameOrCode(String keyword);
}
