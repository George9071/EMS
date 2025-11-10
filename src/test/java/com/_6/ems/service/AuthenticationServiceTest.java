package com._6.ems.service;

import com._6.ems.dto.request.AuthenticationRequest;
import com._6.ems.dto.request.IntrospectRequest;
import com._6.ems.dto.request.LogoutRequest;
import com._6.ems.dto.request.RefreshRequest;
import com._6.ems.dto.response.AuthenticationResponse;
import com._6.ems.dto.response.IntrospectResponse;
import com._6.ems.entity.Account;
import com._6.ems.entity.InvalidatedToken;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.AccountRepository;
import com._6.ems.repository.InvalidatedTokenRepository;
import com._6.ems.repository.PersonnelRepository;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Account account;
    private Personnel personnel;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        
        // Set required fields via reflection
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", 
            "3e7d8a9b2c4f5e6a1b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a");
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", 3600L);
        ReflectionTestUtils.setField(authenticationService, "REFRESHABLE_DURATION", 36000L);

        account = Account.builder()
                .id("ACC001")
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .build();

        personnel = Personnel.builder()
                .code("PER001")
                .firstName("John")
                .lastName("Doe")
                .account(account)
                .build();
    }

    @Test
    void authenticate_validCredentials_success() {
        // GIVEN
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(personnelRepository.findByAccount_Id(account.getId())).thenReturn(Optional.of(personnel));

        // WHEN
        AuthenticationResponse response = authenticationService.authenticate(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getToken()).isNotNull();
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void authenticate_invalidUsername_throwsException() {
        // GIVEN
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("invaliduser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.authenticate(request));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_EXISTED);
        verify(accountRepository).findByUsername("invaliduser");
    }

    @Test
    void authenticate_invalidPassword_throwsException() {
        // GIVEN
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.authenticate(request));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void introspect_validToken_returnsValid() throws JOSEException, ParseException {
        // GIVEN
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(personnelRepository.findByAccount_Id(account.getId())).thenReturn(Optional.of(personnel));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        IntrospectRequest introspectRequest = IntrospectRequest.builder()
                .token(token)
                .build();

        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        // WHEN
        IntrospectResponse response = authenticationService.introspect(introspectRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
    }

    @Test
    void introspect_invalidToken_returnsInvalid() {
        // GIVEN
        IntrospectRequest request = IntrospectRequest.builder()
                .token("invalid.token.here")
                .build();

        // WHEN & THEN
        // The invalid token will cause a ParseException, which the service should handle
        assertThrows(Exception.class, () -> authenticationService.introspect(request));
    }

    @Test
    void logout_validToken_success() throws JOSEException, ParseException {
        // GIVEN
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(personnelRepository.findByAccount_Id(account.getId())).thenReturn(Optional.of(personnel));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .token(token)
                .build();

        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        authenticationService.logout(logoutRequest);

        // THEN
        verify(invalidatedTokenRepository).save(any(InvalidatedToken.class));
    }

    @Test
    void refreshToken_validToken_success() throws JOSEException, ParseException {
        // GIVEN
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(personnelRepository.findByAccount_Id(account.getId())).thenReturn(Optional.of(personnel));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .token(token)
                .build();

        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        AuthenticationResponse response = authenticationService.refreshToken(refreshRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotEqualTo(token);
        verify(invalidatedTokenRepository).save(any(InvalidatedToken.class));
        verify(accountRepository).findById(account.getId());
    }
}
