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
import vn.edu.fpt.medicaldiagnosis.service.AccountService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/accounts")
@Slf4j
public class AccountController {
    @Autowired
    private AccountService accountService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    ApiResponse<AccountResponse> createUser(@RequestBody @Valid AccountCreationRequest request) {
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(accountService.createUser(request));
        return apiResponse;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<AccountResponse> getUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountService.getUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/myInfo")
    ApiResponse<AccountResponse> getMyInfo() {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.getMyInfo())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
//    @PostAuthorize("returnObject.username == authentication.name")
    @GetMapping("/{accountId}")
    AccountResponse getUser(@PathVariable("accountId") String accountId) {
        log.info("In post authorize: ");
        return accountService.getUser(accountId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}")
    AccountResponse updateUser(@PathVariable String accountId, @RequestBody AccountUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountService.updateUser(accountId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{accountId}")
    String deleteUser(@PathVariable String accountId) {
        accountService.deleteUser(accountId);
        return "Account has been deleted";
    }
}
