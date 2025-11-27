package com._6.ems.controller;

import com._6.ems.dto.request.AuthenticationRequest;
import com._6.ems.dto.request.IntrospectRequest;
import com._6.ems.dto.request.LogoutRequest;
import com._6.ems.dto.request.RefreshRequest;
import com._6.ems.dto.response.AuthenticationResponse;
import com._6.ems.dto.response.IntrospectResponse;
import com._6.ems.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthenticationController controller = new AuthenticationController(authenticationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_callsService_andReturnsResult() throws Exception {
        AuthenticationRequest req = AuthenticationRequest.builder().username("u").password("p").build();
        AuthenticationResponse resp = AuthenticationResponse.builder().token("t").build();
        when(authenticationService.authenticate(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.token").value("t"));
    }

    @Test
    void introspect_callsService_andReturns() throws Exception {
        IntrospectRequest req = IntrospectRequest.builder().token("abc").build();
        IntrospectResponse resp = IntrospectResponse.builder().valid(true).build();
        when(authenticationService.introspect(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.valid").value(true));
    }

    @Test
    void refresh_callsService_andReturns() throws Exception {
        RefreshRequest req = RefreshRequest.builder().token("r").build();
        AuthenticationResponse resp = AuthenticationResponse.builder().token("new").build();
        when(authenticationService.refreshToken(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.token").value("new"));
    }

    @Test
    void logout_callsService_andReturnsOk() throws Exception {
        LogoutRequest req = LogoutRequest.builder().token("r").build();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
