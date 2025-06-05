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

    @PostMapping
    ApiResponse<AccountResponse> createAccount(@RequestBody @Valid AccountCreationRequest request) {
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(accountService.createAccount(request));
        return apiResponse;
    }

    @GetMapping
    List<AccountResponse> getAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Accountname: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountService.getAccounts();
    }

    @GetMapping("/myInfo")
    ApiResponse<AccountResponse> getMyInfo() {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.getMyInfo())
                .build();
    }

//    @PostAuthorize("returnObject.Accountname == authentication.name")
    @GetMapping("/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") String accountId) {
        log.info("In post authorize: ");
        return accountService.getAccount(accountId);
    }

    @PutMapping("/{accountId}")
    AccountResponse updateAccount(@PathVariable String accountId, @RequestBody AccountUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Accountname: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountService.updateAccount(accountId, request);
    }

    @DeleteMapping("/{accountId}")
    String deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return "Account has been deleted";
    }
}
