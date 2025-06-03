package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.Role;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.AccountMapper;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;

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

    public AccountResponse createUser(AccountCreationRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Account account = accountMapper.toUser(request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<Role> roles = new HashSet<>();
        Role roleUser = roleRepository.findById("USER").orElseThrow(() -> new RuntimeException("Role not found"));
        roles.add(roleUser);
        account.setRoles(roles);

        account = accountRepository.save(account);
        log.info("User created: {}", account);

        return accountMapper.toUserResponse(account);
    }

    public AccountResponse updateUser(String userId, AccountUpdateRequest request) {
        // Tìm account cũ
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
        Account updatedAccount = accountMapper.updateUser(account, request);

        // Lưu và trả về
        return accountMapper.toUserResponse(accountRepository.save(updatedAccount));
    }

    public void deleteUser(String userId) {
        accountRepository.deleteById(userId);
    }

    public List<AccountResponse> getUsers() {
        List<Account> accounts = accountRepository.findAll();

        return accounts.stream().map(accountMapper::toUserResponse).collect(Collectors.toList());
    }

    public AccountResponse getUser(String id) {
        return accountMapper.toUserResponse(
                accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    public AccountResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();

        Account account = accountRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return accountMapper.toUserResponse(account);
    }
}
