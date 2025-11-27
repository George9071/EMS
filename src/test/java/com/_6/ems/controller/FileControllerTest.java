package com._6.ems.controller;

import com._6.ems.dto.response.FileResponse;
import com._6.ems.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        FileController controller = new FileController(fileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getFileById_returnsFile() throws Exception {
        FileResponse resp = FileResponse.builder().fileId("F1").fileName("doc.pdf").build();
        when(fileService.getFileById("F1")).thenReturn(resp);

        mockMvc.perform(get("/files/F1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fileId").value("F1"));
    }
}
