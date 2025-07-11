package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.StaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentStaffRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.StaffService;
import vn.edu.fpt.medicaldiagnosis.specification.StaffSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static vn.edu.fpt.medicaldiagnosis.enums.Role.PATIENT;
import static vn.edu.fpt.medicaldiagnosis.enums.Role.STAFF;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Transactional
public class StaffServiceImpl implements StaffService {
    StaffRepository staffRepository;
    StaffMapper staffMapper;
    AccountService accountService;
    EmailService emailService;
    DepartmentStaffRepository departmentStaffRepository;
    CodeGeneratorService codeGeneratorService;
    @Override
    public StaffResponse createStaff(StaffCreateRequest staffCreateRequest) {
        log.info("Service: create staff");
        log.info("Creating staff: {}", staffCreateRequest);

        if (staffRepository.existsByEmailAndDeletedAtIsNull(staffCreateRequest.getEmail())) {
            log.info("Email already exists.");
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        if (staffRepository.existsByPhoneAndDeletedAtIsNull(staffCreateRequest.getPhone())) {
            log.info("Phone number already exists.");
            throw new AppException(ErrorCode.STAFF_PHONE_EXISTED);
        }

        String username = accountService.generateUniqueUsername(
                staffCreateRequest.getFirstName(),
                staffCreateRequest.getMiddleName(),
                staffCreateRequest.getLastName()
        );

        String password = DataUtil.generateRandomPassword(10);

        AccountCreationRequest accountRequest = AccountCreationRequest.builder()
                .username(username)
                .password(password)
                .role(STAFF.name())
                .build();

        AccountResponse accountResponse = accountService.createAccount(accountRequest);

        staffCreateRequest.setAccountId(accountResponse.getId());
        Staff staff = staffMapper.toStaff(staffCreateRequest);

        String fullName = (staff.getFirstName() + " " +
                (staff.getMiddleName() != null ? staff.getMiddleName().trim() + " " : "") +
                staff.getLastName()).replaceAll("\\s+", " ").trim();
        staff.setFullName(fullName);

        String staffCode = codeGeneratorService.generateCode("STAFF", "NV", 6);
        staff.setStaffCode(staffCode);

        staff = staffRepository.save(staff);

        log.info("staff created: {}", staff);
        String url = "https://" + TenantContext.getTenantId() + ".datnd.id.vn" + "/home";
        emailService.sendAccountMail(staff.getEmail(), fullName, accountRequest.getUsername(), accountRequest.getPassword(), url);
        return staffMapper.toStaffResponse(staff);
    }

    @Override
    public List<StaffResponse> getAllStaffs() {
        log.info("Service: get all staffs");
        List<Staff> staffs = staffRepository.findAllByDeletedAtIsNull();
        return staffs.stream().map(staffMapper::toStaffResponse).collect(Collectors.toList());
    }

    @Override
    public StaffResponse getStaffById(String id) {
        log.info("Service: get staff by id: {}", id);
        return staffMapper.toStaffResponse(
                staffRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND)));
    }

    @Override
    @Transactional
    public void deleteStaff(String id) {
        log.info("Service: delete staff {}", id);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        staff.setDeletedAt(LocalDateTime.now());
        departmentStaffRepository.deleteByStaffId(id);
        log.info("Deleted department_staffs records for staff {}", id);
        staffRepository.save(staff);
        log.info("Deleted staff: {}", staff);
    }

    @Override
    public StaffResponse updateStaff(String id, StaffUpdateRequest staffUpdateRequest) {
        log.info("Service: update staff {}", id);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staffRepository.existsByEmailAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getEmail(), id)) {
            log.info("Email already exists.");
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        if (staffRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getPhone(), id)) {
            log.info("Phone number already exists.");
            throw new AppException(ErrorCode.STAFF_PHONE_EXISTED);
        }

        staffMapper.updateStaff(staff, staffUpdateRequest);
        String fullName = (staff.getFirstName() + " " +
                (staff.getMiddleName() != null ? staff.getMiddleName().trim() + " " : "") +
                staff.getLastName()).replaceAll("\\s+", " ").trim();
        staff.setFullName(fullName);
        log.info("Staff: {}", staff);
        return staffMapper.toStaffResponse(staffRepository.save(staff));
    }

    @Override
    public Page<StaffResponse> getStaffsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Staff> spec = StaffSpecification.buildSpecification(filters);
        Page<Staff> pageResult = staffRepository.findAll(spec, pageable);
        return pageResult.map(staffMapper::toStaffResponse);
    }

    @Override
    public List<StaffResponse> getStaffNotAssignedToAnyDepartment() {
        log.info("Service: get staff not assigned to any department");
        List<Staff> staffList = staffRepository.findStaffNotAssignedToAnyDepartment();
        return staffList.stream()
                .map(staffMapper::toStaffResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffResponse> searchByNameOrCode(String keyword) {
        log.info("Service: search staff by name or code: {}", keyword);
        List<Staff> staffs = staffRepository.findByFullNameContainingIgnoreCaseOrStaffCodeContainingIgnoreCase(keyword, keyword);
        return staffs.stream().map(staffMapper::toStaffResponse).collect(Collectors.toList());
    }
}
