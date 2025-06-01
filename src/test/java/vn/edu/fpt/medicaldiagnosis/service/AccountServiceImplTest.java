package vn.edu.fpt.medicaldiagnosis.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.Role;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.service.impl.AccountServiceImpl;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class AccountServiceImplTest {
    @Autowired
    private AccountServiceImpl accountServiceImpl;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private RoleRepository roleRepository;

    private AccountCreationRequest accountCreationRequest;
    private AccountResponse accountResponse;
    private Account account;

    private Role role;

    private LocalDate dob;

    @BeforeEach
    public void initData() {
        dob = LocalDate.of(1990, 1, 1);
        accountCreationRequest = AccountCreationRequest.builder()
                .username("test")
                .firstName("john")
                .lastName("james")
                .password("12345678")
                .dob(dob)
                .build();

        accountResponse = AccountResponse.builder()
                .username("journal")
                .firstName("john")
                .lastName("james")
                .dob(dob)
                .build();

        account = Account.builder()
                .id("asnfasdghdsgdsg")
                .username("journal")
                .firstName("john")
                .lastName("james")
                .password("12345678")
                .dob(dob)
                .build();

        role = Role.builder().name("USER").build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        Mockito.when(accountRepository.existsByUsername(anyString())).thenReturn(false);
        Mockito.when(roleRepository.findById(Mockito.any())).thenReturn(Optional.ofNullable(role));
        Mockito.when(accountRepository.save(Mockito.any())).thenReturn(account);

        // WHEN
        AccountResponse response = accountServiceImpl.createUser(accountCreationRequest);

        // THEN
        Assertions.assertThat(response.getUsername()).isEqualTo(accountResponse.getUsername());
    }

    @Test
    void createUser_userExisted_fail() {
        // GIVEN
        Mockito.when(accountRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        AppException exception = assertThrows(AppException.class, () -> accountServiceImpl.createUser(accountCreationRequest));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    @Test
    @WithMockUser(username = "journal")
    void getMyInfo_valid_success() {
        // GIVEN
        Mockito.when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account));

        // WHEN
        AccountResponse response = accountServiceImpl.getMyInfo();
        log.info(response.toString());
        // THEN
        Assertions.assertThat(response.getUsername()).isEqualTo("journal");
    }

    @Test
    @WithMockUser(username = "journal")
    void getMyInfo_userNotFound_error() {
        // GIVEN
        Mockito.when(accountRepository.findByUsername(anyString())).thenReturn(Optional.ofNullable(null));

        // WHEN
        AppException exception = assertThrows(AppException.class, () -> accountServiceImpl.getMyInfo());

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1004);
    }
}
