package com._6.ems.controller;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.dto.response.EmployeeInDepartmentResponse;
import com._6.ems.service.DepartmentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        DepartmentController controller = new DepartmentController(departmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createDepartment_returnsCreated() throws Exception {
        DepartmentCreationRequest req = DepartmentCreationRequest.builder().name("HR").build();
        DepartmentResponse resp = DepartmentResponse.builder().departmentId(10).departmentName("HR").build();
        when(departmentService.createDepartment(req)).thenReturn(resp);

        mockMvc.perform(post("/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.departmentName").value("HR"));

        verify(departmentService).createDepartment(req);
    }

    @Test
    void getDepartmentById_returnsDepartment() throws Exception {
        DepartmentResponse resp = DepartmentResponse.builder().departmentId(1).departmentName("IT").build();
        when(departmentService.getDepartmentById(1)).thenReturn(resp);

        mockMvc.perform(get("/departments").param("id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.departmentId").value(1));
    }

    @Test
    void getAllDepartments_returnsList() throws Exception {
        DepartmentResponse d1 = DepartmentResponse.builder().departmentId(1).departmentName("A").build();
        DepartmentResponse d2 = DepartmentResponse.builder().departmentId(2).departmentName("B").build();
        when(departmentService.getAllDepartments()).thenReturn(Arrays.asList(d1, d2));

        mockMvc.perform(get("/departments/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.length()").value(2));
    }

    @Test
    void getEmployeesInDepartment_callsService() throws Exception {
        EmployeeInDepartmentResponse resp = EmployeeInDepartmentResponse.builder().departmentId(5).departmentName("D5").build();
        when(departmentService.getAllEmployeesInDepartment(5)).thenReturn(resp);

        mockMvc.perform(get("/departments/5/employees"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.departmentId").value(5));
    }

    @Test
    void deleteDepartment_returnsMessage() throws Exception {
        mockMvc.perform(delete("/departments/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Department with id: 3 has been deleted"));
    }
}
