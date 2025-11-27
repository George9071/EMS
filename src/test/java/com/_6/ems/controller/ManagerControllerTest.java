package com._6.ems.controller;

import com._6.ems.dto.request.ManagerCreationRequest;
import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.service.ManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    @Mock
    private ManagerService managerService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ManagerController controller = new ManagerController(managerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createManager_callsService_andReturns() throws Exception {
        ManagerCreationRequest req = ManagerCreationRequest.builder().code("ACC1").department_id("D1").build();
        ManagerResponse resp = ManagerResponse.builder().code("M1").build();
        when(managerService.createManager(req)).thenReturn(resp);

        mockMvc.perform(post("/managers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.code").value("M1"));

        verify(managerService).createManager(req);
    }

    @Test
    void getManagerByCode_returnsManager() throws Exception {
        ManagerResponse resp = ManagerResponse.builder().code("M2").build();
        when(managerService.getManagerByCode("M2")).thenReturn(resp);

        mockMvc.perform(get("/managers").param("code", "M2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.code").value("M2"));
    }

    @Test
    void getAllManager_returnsList() throws Exception {
        ManagerResponse r1 = ManagerResponse.builder().code("A").build();
        ManagerResponse r2 = ManagerResponse.builder().code("B").build();
        when(managerService.getAllManagers()).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/managers/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
    }
}
