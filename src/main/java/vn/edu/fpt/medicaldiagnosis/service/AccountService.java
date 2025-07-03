package vn.edu.fpt.medicaldiagnosis.service;


import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountInfoResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;

import java.util.List;
import java.util.Map;

public interface AccountService {

    AccountResponse createAccount(AccountCreationRequest request);

    AccountResponse updateAccount(String AccountId, AccountUpdateRequest request);

    void deleteAccount(String AccountId);

    List<AccountResponse> getAccounts();

    AccountResponse getAccount(String id);

//    AccountResponse getMyInfo();

    AccountInfoResponse getMyInfo();

    String generateUniqueUsername(String firstName, String middleName, String lastName);

    Page<AccountResponse> getAccountsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    AccountResponse assignRoles(String accountId, List<String> roleNames);
}
