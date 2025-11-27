package com._6.ems.controller;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.EmployeeSimpleResponse;
import com._6.ems.dto.response.ProjectResponse;
import com._6.ems.enums.ProjectStatus;
import com._6.ems.service.ProjectService;
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
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController(projectService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createProject_callsService_andReturns() throws Exception {
        ProjectCreationRequest req = ProjectCreationRequest.builder().name("P").build();
        ProjectResponse resp = ProjectResponse.builder().id("PRJ1").name("P").build();
        when(projectService.createProject(req)).thenReturn(resp);

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("PRJ1"));

        verify(projectService).createProject(req);
    }

    @Test
    void getProjectById_returnsProject() throws Exception {
        ProjectResponse resp = ProjectResponse.builder().id("PRJX").name("X").build();
        when(projectService.getProjectById("PRJX")).thenReturn(resp);

        mockMvc.perform(get("/projects/PRJX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("PRJX"));
    }

    @Test
    void getProjectsByStatus_returnsList() throws Exception {
        ProjectResponse resp = ProjectResponse.builder().id("A").name("A").build();
        when(projectService.searchByStatus(ProjectStatus.PLANNED)).thenReturn(Arrays.asList(resp));

        mockMvc.perform(get("/projects/search/status").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
    void assignEmployeeToProject_callsService() throws Exception {
        when(projectService.assignEmployeeToProject("PRJ1", "E1")).thenReturn(Arrays.asList(EmployeeSimpleResponse.builder().code("E1").build()));

        mockMvc.perform(post("/projects/PRJ1/assign").param("employeeCode", "E1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
    void deleteProject_callsService() throws Exception {
        mockMvc.perform(delete("/projects/TODEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete project success!"));
    }
}
