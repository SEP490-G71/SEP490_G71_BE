package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountInfoResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.AccountMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.specification.AccountSpecification;

import static vn.edu.fpt.medicaldiagnosis.common.DataUtil.removeAccents;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;


    @Transactional
    public AccountResponse createAccount(AccountCreationRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.ACCOUNT_EXISTED);
        }

        Account account = accountMapper.toAccount(request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));

        //  Gán nhiều vai trò
        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)))
                .collect(Collectors.toSet());

        account.setRoles(roles);

        account = accountRepository.save(account);
        log.info("Account created: {}", account);

        return accountMapper.toAccountResponse(account);
    }


    public AccountResponse updateAccount(String AccountId, AccountUpdateRequest request) {
        // Tìm account cũ
        Account account = accountRepository.findById(AccountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Cập nhật danh sách role nếu có
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.getRoles());
            account.setRoles(new HashSet<>(roles));
        }

        // Cập nhật password nếu có (và nên kiểm tra tránh set lại chuỗi rỗng)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật các field còn lại từ mapper
        Account updatedAccount = accountMapper.updateAccount(account, request);

        // Lưu và trả về
        return accountMapper.toAccountResponse(accountRepository.save(updatedAccount));
    }

    @Override
    public void deleteAccount(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDeletedAt(LocalDateTime.now());
        accountRepository.save(account);
    }


    public List<AccountResponse> getAccounts() {
        List<Account> accounts = accountRepository.findAll();

        return accounts.stream().map(accountMapper::toAccountResponse).collect(Collectors.toList());
    }

    public AccountResponse getAccount(String id) {
        return accountMapper.toAccountResponse(
                accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
    }

//    public AccountResponse getMyInfo() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String name = authentication.getName();
//
//        Account account = accountRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
//        return accountMapper.toAccountResponse(account);
//    }

    public AccountInfoResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();

        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(name).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        Set<String> roles = account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        String userId = null;

        if (roles.contains("PATIENT")) {
            Patient patient = patientRepository.findByAccountId(account.getId())
                    .orElse(null);
            if (patient != null) {
                userId = patient.getId();
            }
        }
        else {
            Staff staff = staffRepository.findByAccountId(account.getId())
                    .orElse(null);
            if (staff != null) {
                userId = staff.getId();
            }
        }

        return AccountInfoResponse.builder()
                .username(account.getUsername())
                .userId(userId)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public String generateUniqueUsername(String firstName, String middleName, String lastName) {
        String cleanedLastName = removeAccents(lastName).trim().replaceAll("\\s+", "").toLowerCase();
        String cleanedFirstName = removeAccents(firstName).trim().replaceAll("\\s+", "").toLowerCase();
        String cleanedMiddleName = middleName != null
                ? removeAccents(middleName).trim().replaceAll("\\s+", "").toLowerCase()
                : "";

        // Build base username: ví dụ "Nguyen Van An" → annv
        StringBuilder sb = new StringBuilder();
        sb.append(cleanedLastName);

        if (!cleanedFirstName.isBlank()) {
            sb.append(cleanedFirstName.charAt(0));
        }

        if (!cleanedMiddleName.isBlank()) {
            sb.append(cleanedMiddleName.charAt(0));
        }

        String base = sb.toString();
        List<String> existingUsernames = accountRepository.findUsernamesByPrefix(base);

        if (!existingUsernames.contains(base)) return base;

        int maxSuffix = existingUsernames.stream()
                .map(name -> name.replace(base, ""))
                .filter(suffix -> suffix.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.format("%s%02d", base, maxSuffix + 1); // ví dụ: annv03
    }


    @Override
    public Page<AccountResponse> getAccountsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Account> spec = AccountSpecification.buildSpecification(filters);
        Page<Account> accounts = accountRepository.findAll(spec, pageable);

        return accounts.map(accountMapper::toAccountResponse);
    }

    @Override
    @Transactional
    public AccountResponse assignRoles(String accountId, List<String> roleNames) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Lấy tất cả role có thật từ DB
        List<Role> foundRoles = roleRepository.findAllById(roleNames);
        List<String> foundRoleNames = foundRoles.stream().map(Role::getName).toList();

        // So sánh để tìm role không tồn tại
        List<String> missingRoles = roleNames.stream()
                .filter(r -> !foundRoleNames.contains(r))
                .toList();

        if (!missingRoles.isEmpty()) {
            log.error("Role not found: {}", missingRoles);
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        account.setRoles(new HashSet<>(foundRoles));
        accountRepository.save(account);

        return accountMapper.toAccountResponse(account);
    }
}
