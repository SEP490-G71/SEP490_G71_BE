package vn.edu.fpt.medicaldiagnosis.service;


import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createUser(AccountCreationRequest request);

    AccountResponse updateUser(String userId, AccountUpdateRequest request);

    void deleteUser(String userId);

    List<AccountResponse> getUsers();

    AccountResponse getUser(String id);

    AccountResponse getMyInfo();
}
