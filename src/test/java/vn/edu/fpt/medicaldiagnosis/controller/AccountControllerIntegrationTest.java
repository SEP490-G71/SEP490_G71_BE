package vn.edu.fpt.medicaldiagnosis.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@Testcontainers
public class AccountControllerIntegrationTest {
    @Container
    static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.0.39");

    @Autowired
    private MockMvc mockMvc;

    private AccountCreationRequest accountCreationRequest;
    private AccountResponse accountResponse;
    private LocalDate dob;

//    @BeforeEach
//    public void initData() {
//        dob = LocalDate.of(1990, 1, 1);
//        accountCreationRequest = AccountCreationRequest.builder()
//                .username("test1")
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
//
//    @Test
//    void createUser_validRequest_success() throws Exception {
//        // GIVEN
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        String content = objectMapper.writeValueAsString(accountCreationRequest);
//
//        // WHEN, THEN
//        mockMvc.perform(MockMvcRequestBuilders.post("/users")
//                        .contentType("application/json")
//                        .content(content))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("code").value("1000"))
//                .andExpect(MockMvcResultMatchers.jsonPath("result.username").value("test1"));
//    }
}
