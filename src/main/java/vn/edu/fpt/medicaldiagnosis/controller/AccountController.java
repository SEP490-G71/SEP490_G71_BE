package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.service.impl.AccountServiceImpl;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
public class AccountController {
    @Autowired
    private AccountServiceImpl accountServiceImpl;

    @PostMapping
    ApiResponse<AccountResponse> createUser(@RequestBody @Valid AccountCreationRequest request) {
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(accountServiceImpl.createUser(request));
        return apiResponse;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<AccountResponse> getUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountServiceImpl.getUsers();
    }

    @GetMapping("/myInfo")
    ApiResponse<AccountResponse> getMyInfo() {
        return ApiResponse.<AccountResponse>builder()
                .result(accountServiceImpl.getMyInfo())
                .build();
    }

    @PostAuthorize("returnObject.username == authentication.name")
    @GetMapping("/{userId}")
    AccountResponse getUser(@PathVariable("userId") String userId) {
        log.info("In post authorize: ");
        return accountServiceImpl.getUser(userId);
    }

    @PutMapping("/{userId}")
    AccountResponse updateUser(@PathVariable String userId, @RequestBody AccountUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountServiceImpl.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    String deleteUser(@PathVariable String userId) {
        accountServiceImpl.deleteUser(userId);
        return "User has been deleted";
    }
}
