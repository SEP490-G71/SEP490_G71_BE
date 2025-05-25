package com.devintel.identityservice.service;

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

import com.devintel.identityservice.dto.request.UserCreationRequest;
import com.devintel.identityservice.dto.response.UserResponse;
import com.devintel.identityservice.entity.Role;
import com.devintel.identityservice.entity.User;
import com.devintel.identityservice.exception.AppException;
import com.devintel.identityservice.repository.RoleRepository;
import com.devintel.identityservice.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    private UserCreationRequest userCreationRequest;
    private UserResponse userResponse;
    private User user;

    private Role role;

    private LocalDate dob;

    @BeforeEach
    public void initData() {
        dob = LocalDate.of(1990, 1, 1);
        userCreationRequest = UserCreationRequest.builder()
                .username("test")
                .firstName("john")
                .lastName("james")
                .password("12345678")
                .dob(dob)
                .build();

        userResponse = UserResponse.builder()
                .username("journal")
                .firstName("john")
                .lastName("james")
                .dob(dob)
                .build();

        user = User.builder()
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
        Mockito.when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Mockito.when(roleRepository.findById(Mockito.any())).thenReturn(Optional.ofNullable(role));
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(user);

        // WHEN
        UserResponse response = userService.createUser(userCreationRequest);

        // THEN
        Assertions.assertThat(response.getUsername()).isEqualTo(userResponse.getUsername());
    }

    @Test
    void createUser_userExisted_fail() {
        // GIVEN
        Mockito.when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        AppException exception = assertThrows(AppException.class, () -> userService.createUser(userCreationRequest));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    @Test
    @WithMockUser(username = "journal")
    void getMyInfo_valid_success() {
        // GIVEN
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        // WHEN
        UserResponse response = userService.getMyInfo();
        log.info(response.toString());
        // THEN
        Assertions.assertThat(response.getUsername()).isEqualTo("journal");
    }

    @Test
    @WithMockUser(username = "journal")
    void getMyInfo_userNotFound_error() {
        // GIVEN
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.ofNullable(null));

        // WHEN
        AppException exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1004);
    }
}
