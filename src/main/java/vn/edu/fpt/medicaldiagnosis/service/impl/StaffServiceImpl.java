package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Role;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.StaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.StaffService;
import vn.edu.fpt.medicaldiagnosis.specification.StaffSpecification;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

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
    CodeGeneratorService codeGeneratorService;
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    EmailTaskRepository emailTaskRepository;
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
                .roles(staffCreateRequest.getRoleNames())
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
        EmailTask emailTask = buildAccountEmailTask(staff, accountRequest.getUsername(), accountRequest.getPassword(), TenantContext.getTenantId());
        emailTaskRepository.save(emailTask);
        return mapToStaffResponseWithRoles(staff);
    }

    @Override
    public List<StaffResponse> getAllStaffs() {
        log.info("Service: get all staffs");
        List<Staff> staffs = staffRepository.findAllByDeletedAtIsNull();
        return staffs.stream()
                .map(this::mapToStaffResponseWithRoles)
                .collect(Collectors.toList());
    }

    @Override
    public StaffResponse getStaffById(String id) {
        log.info("Service: get staff by id: {}", id);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        return mapToStaffResponseWithRoles(staff);
    }

    @Override
    @Transactional
    public void deleteStaff(String id) {
        log.info("Service: delete staff {}", id);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        staff.setDeletedAt(LocalDateTime.now());
        staffRepository.save(staff);
        log.info("Deleted staff: {}", staff);

        if (staff.getAccountId() != null) {
            Optional<Account> optionalAccount = accountRepository.findByIdAndDeletedAtIsNull(staff.getAccountId());
            optionalAccount.ifPresent(account -> {
                account.setDeletedAt(LocalDateTime.now());
                accountRepository.save(account);
                log.info("Deleted linked account: {}", account.getUsername());
            });
        }
    }

    @Override
    public StaffResponse updateStaff(String id, StaffUpdateRequest staffUpdateRequest) {
        log.info("Service: update staff {}", id);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staffRepository.existsByEmailAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getEmail(), id)) {
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        if (staffRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getPhone(), id)) {
            throw new AppException(ErrorCode.STAFF_PHONE_EXISTED);
        }

        staffMapper.updateStaff(staff, staffUpdateRequest);
        String fullName = (staff.getFirstName() + " " +
                (staff.getMiddleName() != null ? staff.getMiddleName().trim() + " " : "") +
                staff.getLastName()).replaceAll("\\s+", " ").trim();
        staff.setFullName(fullName);

        // ✅ Cập nhật roles nếu có
        if (staff.getAccountId() != null && staffUpdateRequest.getRoleNames() != null) {
            Account account = accountRepository.findById(staff.getAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

            Set<Role> updatedRoles = staffUpdateRequest.getRoleNames().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)))
                    .collect(Collectors.toSet());

            account.setRoles(updatedRoles);
            accountRepository.save(account);
        }

        return mapToStaffResponseWithRoles(staffRepository.save(staff));
    }

    @Override
    public Page<StaffResponse> getStaffsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get paged staffs");
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Staff> spec = StaffSpecification.buildSpecification(filters);
        Page<Staff> pageResult = staffRepository.findAll(spec, pageable);
        return pageResult.map(this::mapToStaffResponseWithRoles);
    }

    private String getExpectedRoleForDepartmentType(DepartmentType type) {
        return switch (type) {
            case CONSULTATION -> "DOCTOR";
            case LABORATORY -> "TECHNICIAN";
            case ADMINISTRATION -> "ADMIN"; // hoặc return "ADMIN", nếu có
        };
    }


    @Override
    public List<StaffResponse> getStaffNotAssignedToAnyDepartment(String keyword, DepartmentType departmentType) {
        log.info("Service: get staff not assigned to any department, keyword = {}, type = {}", keyword, departmentType);

        String roleName = getExpectedRoleForDepartmentType(departmentType);

        List<Staff> staffList = staffRepository.findUnassignedStaffByRoleAndKeyword(roleName, keyword);

        return staffList.stream()
                .map(this::mapToStaffResponseWithRoles)
                .collect(Collectors.toList());
    }


    @Override
    public List<StaffResponse> searchByNameOrCode(String keyword) {
        log.info("Service: search staff by name or code: {}", keyword);
        List<Staff> staffs = staffRepository.findByFullNameContainingIgnoreCaseOrStaffCodeContainingIgnoreCase(keyword, keyword);
        return staffs.stream()
                .map(this::mapToStaffResponseWithRoles)
                .collect(Collectors.toList());
    }

    private StaffResponse mapToStaffResponseWithRoles(Staff staff) {
        StaffResponse response = staffMapper.toStaffResponse(staff);
        if (staff.getAccountId() != null) {
            Optional<Account> accountOpt = accountRepository.findById(staff.getAccountId());
            accountOpt.ifPresent(account -> {
                List<String> roleNames = account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList());
                response.setRoles(roleNames);
            });
        }
        return response;
    }

    private EmailTask buildAccountEmailTask(Staff staff, String username, String password, String tenantId) {
        String url = "https://" + tenantId + ".datnd.id.vn/home";
        String content;

        try {
            ClassPathResource resource = new ClassPathResource("templates/account-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template
                    .replace("{{name}}", staff.getFullName())
                    .replace("{{username}}", username)
                    .replace("{{password}}", password)
                    .replace("{{url}}", url);
        } catch (Exception e) {
            content = String.format("Xin chào %s,\n\nTài khoản đã được tạo:\nUsername: %s\nPassword: %s\n\nĐăng nhập tại: %s\n\nTrân trọng.",
                    staff.getFullName(), username, password, url);
            log.warn("[{}] Không thể load template account-created-email.html: {}", tenantId, e.getMessage());
        }

        return EmailTask.builder()
                .id(UUID.randomUUID().toString())
                .emailTo(staff.getEmail())
                .subject("Tài khoản của bạn đã được tạo")
                .content(content)
                .retryCount(0)
                .status(Status.PENDING)
                .build();
    }

}
