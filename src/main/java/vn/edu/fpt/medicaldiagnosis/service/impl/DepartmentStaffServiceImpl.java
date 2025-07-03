package vn.edu.fpt.medicaldiagnosis.service.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentStaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentStaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentStaff;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.DepartmentStaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentRepository;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentStaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentStaffService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Transactional
public class DepartmentStaffServiceImpl implements DepartmentStaffService {
    DepartmentRepository departmentRepository;
    StaffRepository staffRepository;
    DepartmentStaffRepository departmentStaffRepository;
    DepartmentStaffMapper departmentStaffMapper;
    AccountRepository accountRepository;
    @Override
    public List<DepartmentStaffResponse> assignStaffsToDepartment(DepartmentStaffCreateRequest request) {
        log.info("Service: assign staffs to department");
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Xóa toàn bộ quyền cũ của phòng này
        departmentStaffRepository.deleteByDepartmentId(department.getId());

        // Tạo danh sách mới
        List<DepartmentStaffResponse> responses = new ArrayList<>();
        for (DepartmentStaffCreateRequest.StaffPosition sp : request.getStaffPositions()) {
            Staff staff = staffRepository.findById(sp.getStaffId())
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

            DepartmentStaff entity = DepartmentStaff.builder()
                    .department(department)
                    .staff(staff)
                    .position(sp.getPosition())
                    .build();

            entity = departmentStaffRepository.save(entity);
            responses.add(departmentStaffMapper.toDepartmentStaffResponse(entity));
        }

        return responses;
    }

    @Override
    public List<DepartmentStaffResponse> getStaffsByDepartmentId(String departmentId) {
        log.info("Service: get staffs by department id");
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        List<DepartmentStaff> departmentStaffs = departmentStaffRepository.findByDepartmentId(departmentId);

        return departmentStaffs.stream()
                .map(departmentStaffMapper::toDepartmentStaffResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentStaffResponse getMyDepartmentInfo(String username) {
        log.info("Service: get my department info");
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Staff staff = staffRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        DepartmentStaff deptStaff = departmentStaffRepository.findByStaffId(staff.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return DepartmentStaffResponse.builder()
                .id(UUID.fromString((deptStaff.getId())))
                .staffId(UUID.fromString(staff.getId()))
                .departmentId(UUID.fromString(deptStaff.getDepartment().getId()))
                .departmentName(deptStaff.getDepartment().getName())
                .staffName(staff.getFullName())
                .position(deptStaff.getPosition())
                .build();
    }
}
