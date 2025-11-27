package com._6.ems.controller;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        EmployeeController controller = new EmployeeController(employeeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createEmployee_valid_request_returnsResult() throws Exception {
        EmployeeCreationRequest req = EmployeeCreationRequest.builder().department_id(1).build();
        EmployeeResponse resp = EmployeeResponse.builder().employee_code("EMP001").department_id(1).build();

        when(employeeService.createEmployee(req)).thenReturn(resp);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.employee_code").value("EMP001"))
                .andExpect(jsonPath("$.message").value("Add new employee success!"));

        verify(employeeService).createEmployee(req);
    }

    @Test
    void getEmployeeByCode_returnsEmployee() throws Exception {
        EmployeeResponse resp = EmployeeResponse.builder().employee_code("EMP002").department_id(2).build();
        when(employeeService.getEmployeeByCode("EMP002")).thenReturn(resp);

        mockMvc.perform(get("/employees/code").param("code", "EMP002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.employee_code").value("EMP002"));
    }

    @Test
    void getAllEmployee_returnsList() throws Exception {
        EmployeeResponse r1 = EmployeeResponse.builder().employee_code("E1").build();
        EmployeeResponse r2 = EmployeeResponse.builder().employee_code("E2").build();
        when(employeeService.getAllEmployee()).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/employees/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
    }

    @Test
    void createEmployee_callsService_withSameBody() throws Exception {
        EmployeeCreationRequest req = EmployeeCreationRequest.builder().department_id(5).build();
        EmployeeResponse resp = EmployeeResponse.builder().employee_code("EMP005").build();
        when(employeeService.createEmployee(req)).thenReturn(resp);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(employeeService).createEmployee(req);
    }

    @Test
    void getEmployeeByCode_messageContainsCode() throws Exception {
        EmployeeResponse resp = EmployeeResponse.builder().employee_code("X123").build();
        when(employeeService.getEmployeeByCode("X123")).thenReturn(resp);

        mockMvc.perform(get("/employees/code").param("code", "X123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Get employee with code: X123 success!"));
    }
}
