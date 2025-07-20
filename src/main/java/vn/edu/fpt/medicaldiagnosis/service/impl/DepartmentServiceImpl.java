package vn.edu.fpt.medicaldiagnosis.service.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.AssignStaffRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.DepartmentMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.StaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentService;
import vn.edu.fpt.medicaldiagnosis.specification.DepartmentSpecification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DepartmentServiceImpl implements DepartmentService {
    DepartmentRepository departmentRepository;
    DepartmentMapper departmentMapper;
    MedicalServiceRepository medicalServiceRepository;
    StaffRepository staffRepository;
    StaffMapper staffMapper;
    AccountRepository accountRepository;
    SpecializationRepository specializationRepository;
    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest departmentCreateRequest) {
        log.info("Service: create department");
        log.info("Creating department: {}", departmentCreateRequest);
        if (departmentRepository.existsByRoomNumberAndDeletedAtIsNull(departmentCreateRequest.getRoomNumber())) {
            log.info("Room number already exists.");
            throw new AppException(ErrorCode.DEPARTMENT_ROOM_EXISTED);
        }
        Specialization specialization = null;

        // ✅ Nếu là CONSULTATION, phải có specialization
        if (DepartmentType.CONSULTATION.equals(departmentCreateRequest.getType())) {
            if (departmentCreateRequest.getSpecializationId() == null) {
                throw new AppException(ErrorCode.DEPARTMENT_SPECIALIZATION_REQUIRED);
            }

            specialization = specializationRepository.findById(departmentCreateRequest.getSpecializationId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));
        }

        // Tạo entity từ request
        Department department = departmentMapper.toDepartment(departmentCreateRequest);
        if (specialization != null) {
            department.setSpecialization(specialization);
        }
        // Lưu vào DB
        department = departmentRepository.save(department);

        log.info("Department created: {}", department);

        // Trả response
        return departmentMapper.toDepartmentResponse(department);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        log.info("Service: get all departments");
        List<Department> departments = departmentRepository.findAllByDeletedAtIsNull();
        return departments.stream().map(departmentMapper::toDepartmentResponse).collect(Collectors.toList());
    }

    @Override
    public DepartmentDetailResponse getDepartmentById(String id) {
        log.info("Service: get department by id: {}", id);

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        DepartmentDetailResponse response = departmentMapper.toDepartmentDetailResponse(department);
        List<Staff> staffList = staffRepository.findByDepartmentId(department.getId());
        response.setStaffs(staffList.stream().map(staffMapper::toBasicResponse).toList());

        return response;
    }


    @Override
    @Transactional
    public void deleteDepartment(String id) {
        log.info("Service: delete department {}", id);

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // ✅ Gỡ liên kết các dịch vụ y tế khỏi phòng ban (set department = null)
        List<MedicalService> services = medicalServiceRepository.findByDepartmentIdAndDeletedAtIsNull(id);
        for (MedicalService service : services) {
            service.setDepartment(null); // 👈 Gỡ liên kết, không xoá mềm
        }
        medicalServiceRepository.saveAll(services);
        log.info("Unlinked {} medical service(s) from department {}", services.size(), id);

        // ✅ Huỷ liên kết nhân viên
        List<Staff> staffs = staffRepository.findByDepartmentId(id);
        for (Staff staff : staffs) {
            staff.setDepartment(null);
        }
        staffRepository.saveAll(staffs);
        log.info("Unlinked {} staff(s) from department {}", staffs.size(), id);

        // ✅ Xoá mềm phòng ban
        department.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(department);
        log.info("Soft deleted department {}", id);
    }



    @Override
    public DepartmentResponse updateDepartment(String id, DepartmentUpdateRequest departmentUpdateRequest) {
        log.info("Service: update department {}", id);
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Kiểm tra phòng đã tồn tại nếu số phòng bị thay đổi
        if (departmentUpdateRequest.getRoomNumber() != null &&
                !departmentUpdateRequest.getRoomNumber().equals(department.getRoomNumber()) &&
                departmentRepository.existsByRoomNumberAndDeletedAtIsNull(departmentUpdateRequest.getRoomNumber())) {
            throw new AppException(ErrorCode.DEPARTMENT_ROOM_EXISTED);
        }

        // Nếu type là CONSULTATION thì bắt buộc phải có specializationId
        DepartmentType newType = departmentUpdateRequest.getType() != null
                ? departmentUpdateRequest.getType()
                : department.getType(); // nếu không gửi lên thì giữ nguyên

        // ✅ Nếu là phòng khám thì specialization là bắt buộc
        if (DepartmentType.CONSULTATION.equals(newType)) {
            if (departmentUpdateRequest.getSpecializationId() == null) {
                throw new AppException(ErrorCode.DEPARTMENT_SPECIALIZATION_REQUIRED);
            }

            // ✅ Kiểm tra specializationId có tồn tại
            if (!specializationRepository.existsById(departmentUpdateRequest.getSpecializationId())) {
                throw new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND);
            }

            // ✅ Gán specialization nếu thay đổi
            if (department.getSpecialization() == null ||
                    !department.getSpecialization().getId().equals(departmentUpdateRequest.getSpecializationId())) {
                Specialization specialization = specializationRepository.findById(departmentUpdateRequest.getSpecializationId())
                        .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));
                department.setSpecialization(specialization);
            }
        } else {
            // ✅ Nếu không phải phòng khám thì clear specialization
            department.setSpecialization(null);
        }

        // Cập nhật dữ liệu
        departmentMapper.updateDepartment(department, departmentUpdateRequest);
        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }


    @Override
    public Page<DepartmentResponse> getDepartmentsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get departments paged");
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Department> spec = DepartmentSpecification.buildSpecification(filters);

        Page<Department> pageResult = departmentRepository.findAll(spec, pageable);
        return pageResult.map(departmentMapper::toDepartmentResponse);
    }

    @Override
    public DepartmentDetailResponse assignStaffsToDepartment(String departmentId, AssignStaffRequest request) {
        log.info("Service: assign staffs to department {}", departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Huỷ liên kết nhân viên cũ
        List<Staff> oldStaffs = staffRepository.findByDepartmentId(departmentId);
        for (Staff s : oldStaffs) {
            s.setDepartment(null);
        }
        staffRepository.saveAll(oldStaffs);

        // Gán nhân viên mới
        List<Staff> newAssignedStaffs = new ArrayList<>();
        for (String staffId : request.getStaffIds()) {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
            staff.setDepartment(department);
            newAssignedStaffs.add(staff);
        }
        staffRepository.saveAll(newAssignedStaffs);

        // Tạo response chi tiết
        DepartmentDetailResponse response = departmentMapper.toDepartmentDetailResponse(department);
        response.setStaffs(newAssignedStaffs.stream()
                .map(staffMapper::toBasicResponse)
                .collect(Collectors.toList()));

        return response;
    }



    @Override
    public DepartmentResponse getMyDepartment(String username) {
        log.info("Service: get my department");
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Staff staff = staffRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Department department = staff.getDepartment();
        if (department == null) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        // Chỉ map department, KHÔNG lấy danh sách nhân viên
        return departmentMapper.toDepartmentResponse(department);
    }

    @Override
    public DepartmentResponse getByTypeAndRoomNumberAndSpecializationId(String type, String roomNumber, String specializationId) {
        log.info("Service: get department by type = {}, room = {}, specializationId = {}", type, roomNumber, specializationId);

        Department department = departmentRepository
                .findByTypeAndRoomNumberAndSpecializationId(type, roomNumber, specializationId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return departmentMapper.toDepartmentResponse(department);
    }

}
