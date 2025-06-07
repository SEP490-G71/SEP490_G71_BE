package vn.edu.fpt.medicaldiagnosis.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.service.impl.AccountServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountServiceImpl accountServiceImpl;

    private AccountCreationRequest accountCreationRequest;
    private AccountResponse accountResponse;
    private LocalDate dob;

    @BeforeEach
//    public void initData() {
//        dob = LocalDate.of(1990, 1, 1);
//        accountCreationRequest = AccountCreationRequest.builder()
//                .username("test")
//                .firstName("john")
//                .lastName("james")
//                .password("12345678")
//                .dob(dob)
//                .build();
//
//        accountResponse = AccountResponse.builder()
//                .username("test")
//                .firstName("john")
//                .lastName("james")
//                .dob(dob)
//                .build();
//    }

    @Test
    void createUser_validRequest() throws Exception {
        // GIVEN
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(accountCreationRequest);

        Mockito.when(accountServiceImpl.createAccount(ArgumentMatchers.any())).thenReturn(accountResponse);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value("1000"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.username").value("test"));
        // THEN
    }

    @Test
    void createUser_invalidRequest() throws Exception {
        // GIVEN
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(accountCreationRequest);

        Mockito.when(accountServiceImpl.createAccount(ArgumentMatchers.any())).thenReturn(accountResponse);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value("1000"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.username").value("test"));
        // THEN
    }

    @Test
    void createUser_passwordInvalid_fail() throws Exception {
        // GIVEN
        accountCreationRequest.setPassword("1234");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(accountCreationRequest);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value("1003"))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Password must be at least 8 characters"));
        // THEN
    }
}
