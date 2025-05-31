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
import vn.edu.fpt.medicaldiagnosis.mapper.UserMapper;
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
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AccountResponse createUser(AccountCreationRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Account account = userMapper.toUser(request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<Role> roles = new HashSet<>();
        Role roleUser = roleRepository.findById("USER").orElseThrow(() -> new RuntimeException("Role not found"));
        roles.add(roleUser);
        account.setRoles(roles);

        account = accountRepository.save(account);
        log.info("User created: {}", account);

        return userMapper.toUserResponse(account);
    }

    public AccountResponse updateUser(String userId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<Role> roles = roleRepository.findAllById(request.getRoles());
        account.setRoles(new HashSet<>(roles));
        account.setPassword(passwordEncoder.encode(request.getPassword()));

        Account updatedAccount = userMapper.updateUser(account, request);

        return userMapper.toUserResponse(accountRepository.save(updatedAccount));
    }

    public void deleteUser(String userId) {
        accountRepository.deleteById(userId);
    }

    public List<AccountResponse> getUsers() {
        List<Account> accounts = accountRepository.findAll();

        return accounts.stream().map(userMapper::toUserResponse).collect(Collectors.toList());
    }

    public AccountResponse getUser(String id) {
        return userMapper.toUserResponse(
                accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    public AccountResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();

        Account account = accountRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(account);
    }
}
