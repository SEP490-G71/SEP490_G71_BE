package vn.edu.fpt.medicaldiagnosis.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AssignRolesRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountInfoResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/accounts")
@Slf4j
public class AccountController {
    @Autowired
    private AccountService accountService;
    @PutMapping("/{accountId}/roles")
    public ApiResponse<AccountResponse> assignRolesToAccount(
            @PathVariable String accountId,
            @RequestBody AssignRolesRequest request
    ) {
        AccountResponse response = accountService.assignRoles(accountId, request.getRoleNames());
        return ApiResponse.<AccountResponse>builder().result(response).build();
    }

    @GetMapping
    ApiResponse<PagedResponse<AccountResponse>> getAccounts(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        log.info("Controller: get accounts with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);
        Page<AccountResponse> result = accountService.getAccountsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<AccountResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<AccountResponse>>builder().result(response).build();
    }

    @PostMapping
    ApiResponse<AccountResponse> createAccount(@RequestBody @Valid AccountCreationRequest request) {
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(accountService.createAccount(request));
        return apiResponse;
    }

    @GetMapping("/all")
    List<AccountResponse> getAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Accountname: {}", authentication.getName());
        authentication.getAuthorities().forEach(r -> log.info("Role: {}", r.getAuthority()));
        return accountService.getAccounts();
    }

    @GetMapping("/myInfo")
    ApiResponse<AccountInfoResponse> getMyInfo() {
        return ApiResponse.<AccountInfoResponse>builder()
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
