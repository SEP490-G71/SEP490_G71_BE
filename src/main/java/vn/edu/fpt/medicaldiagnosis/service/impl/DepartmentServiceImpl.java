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
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.DepartmentMapper;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentRepository;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentStaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalServiceRepository;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentService;
import vn.edu.fpt.medicaldiagnosis.specification.DepartmentSpecification;

import java.time.LocalDateTime;
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
    DepartmentStaffRepository departmentStaffRepository;
    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest departmentCreateRequest) {
        log.info("Service: create department");
        log.info("Creating department: {}", departmentCreateRequest);
        if (departmentRepository.existsByRoomNumberAndDeletedAtIsNull(departmentCreateRequest.getRoomNumber())) {
            log.info("Room number already exists.");
            throw new AppException(ErrorCode.DEPARTMENT_ROOM_EXISTED);
        }

        // Tạo entity từ request
        Department department = departmentMapper.toDepartment(departmentCreateRequest);

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
    public DepartmentResponse getDepartmentById(String id) {
        log.info("Service: get department by id: {}", id);
        return departmentMapper.toDepartmentResponse(
                departmentRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND)));
    }

    @Override
    @Transactional
    public void deleteDepartment(String id) {
        log.info("Service: delete department {}", id);
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        List<MedicalService> services = medicalServiceRepository.findByDepartmentIdAndDeletedAtIsNull(id);
        for (MedicalService service : services) {
            service.setDeletedAt(LocalDateTime.now());
        }
        log.info("Deleting medical services: {}", services);
        medicalServiceRepository.saveAll(services);
        departmentStaffRepository.deleteByDepartmentId(id);
        log.info("Deleted department_staffs records for department {}", id);
        department.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(department);
    }

    @Override
    public DepartmentResponse updateDepartment(String id, DepartmentUpdateRequest departmentUpdateRequest) {
                log.info("Service: update department {}", id);
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        if (departmentUpdateRequest.getRoomNumber() != null &&
                !departmentUpdateRequest.getRoomNumber().equals(department.getRoomNumber()) &&
                departmentRepository.existsByRoomNumberAndDeletedAtIsNull(departmentUpdateRequest.getRoomNumber())) {

            throw new AppException(ErrorCode.DEPARTMENT_ROOM_EXISTED);
        }
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
}
