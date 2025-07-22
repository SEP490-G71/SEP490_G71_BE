package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;

import java.util.List;

public interface AccountControlService {
    List<AccountResponse> getAllAccounts();
}
