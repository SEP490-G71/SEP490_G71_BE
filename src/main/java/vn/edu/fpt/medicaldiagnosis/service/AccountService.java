package vn.edu.fpt.medicaldiagnosis.service;


import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(AccountCreationRequest request);

    AccountResponse updateAccount(String AccountId, AccountUpdateRequest request);

    void deleteAccount(String AccountId);

    List<AccountResponse> getAccounts();

    AccountResponse getAccount(String id);

    AccountResponse getMyInfo();

    String generateUniqueUsername(String firstName, String middleName, String lastName);
}
