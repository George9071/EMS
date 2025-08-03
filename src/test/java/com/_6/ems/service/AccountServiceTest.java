package com._6.ems.service;

import com._6.ems.dto.request.AccountCreationRequest;
import com._6.ems.dto.response.AccountResponse;
import com._6.ems.entity.Account;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.repository.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource("/test.properties")
public class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @MockBean
    private AccountRepository accountRepository;

    private AccountCreationRequest request;
    private AccountResponse response;
    private Account account;
    private LocalDate dob;

    @BeforeEach
    void initData(){
        request = AccountCreationRequest.builder()
                .username("john123")
                .password("12345678")
                .build();

        response = AccountResponse.builder()
                .id("cf0600f538b3")
                .username("john123")
                .build();

        account = Account.builder()
                .id("cf0600f538b3")
                .username("john123")
                .role(Role.EMPLOYEE)
                .build();
    }

    @Test
    void createAccount_validRequest_success(){
        // GIVEN
        when(accountRepository.existsByUsername(anyString())).thenReturn(false);
        when(accountRepository.save(any())).thenReturn(account);

        // WHEN
        var response = accountService.createAccount(request);

        // THEN
        Assertions.assertThat(response.getId()).isEqualTo("cf0600f538b3");
        Assertions.assertThat(response.getUsername()).isEqualTo("john123");
        Assertions.assertThat(response.getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    void createAccount_accountExisted_fail(){
        // GIVEN
        when(accountRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class,
                () -> accountService.createAccount(request));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode())
                .isEqualTo(1002);
    }
}
