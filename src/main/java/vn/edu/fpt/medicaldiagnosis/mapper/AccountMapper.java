package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import vn.edu.fpt.medicaldiagnosis.common.RoleMapperHelper;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;

@Mapper(componentModel = "spring", uses = RoleMapperHelper.class)
public interface AccountMapper {
    Account toAccount(AccountCreationRequest request);

    AccountResponse toAccountResponse(Account account);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    Account updateAccount(@MappingTarget Account account, AccountUpdateRequest request);
}
