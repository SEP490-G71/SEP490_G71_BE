package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentStaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentStaffResponse;

import java.util.List;
import java.util.UUID;

public interface DepartmentStaffService {
    List<DepartmentStaffResponse> assignStaffsToDepartment(DepartmentStaffCreateRequest request);

    List<DepartmentStaffResponse> getStaffsByDepartmentId(UUID departmentId);
}
