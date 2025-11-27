package com._6.ems.controller;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.request.TaskUpdateRequest;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.service.TaskService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TaskController controller = new TaskController(taskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createTask_returnsResult() throws Exception {
        TaskCreationRequest req = TaskCreationRequest.builder().title("T1").build();
        TaskResponse resp = TaskResponse.builder().id("TID").title("T1").build();
        when(taskService.createTask(req)).thenReturn(resp);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("TID"));
    }

    @Test
    void getTaskById_returnsTask() throws Exception {
        TaskResponse resp = TaskResponse.builder().id("TK").title("Title").build();
        when(taskService.getTaskById("TK")).thenReturn(resp);

        mockMvc.perform(get("/tasks/TK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("TK"));
    }

    @Test
    void getTasksByProject_returnsList() throws Exception {
        TaskResponse r = TaskResponse.builder().id("A").title("A").build();
        when(taskService.getTasksByProject("PRJ1")).thenReturn(Arrays.asList(r));

        mockMvc.perform(get("/tasks/project").param("projectId", "PRJ1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
    void assignTask_callsService() throws Exception {
        TaskResponse resp = TaskResponse.builder().id("T1").title("t").build();
        when(taskService.assignTask("T1", "E1")).thenReturn(resp);

        mockMvc.perform(put("/tasks/assign").param("taskId", "T1").param("employeeCode", "E1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("T1"));
    }

    @Test
    void deleteTask_returnsMessage() throws Exception {
        mockMvc.perform(delete("/tasks/TDEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete task success!"));
    }
}
