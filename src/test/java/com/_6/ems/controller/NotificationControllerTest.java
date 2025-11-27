package com._6.ems.controller;

import com._6.ems.dto.request.AdminNotificationRequest;
import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.SendNotificationResponse;
import com._6.ems.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void sendNotification_callsService_andReturns() throws Exception {
        NotificationRequest req = NotificationRequest.builder().subject("s").message("m").build();
        SendNotificationResponse resp = SendNotificationResponse.builder().build();
        when(notificationService.sendNotification(org.mockito.ArgumentMatchers.any())).thenReturn(resp);

        mockMvc.perform(post("/notification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Send notification success"));
    }

    @Test
    void sendNotificationToAllExceptAdmin_returnsOk() throws Exception {
        AdminNotificationRequest req = AdminNotificationRequest.builder().subject("s").message("m").build();
        SendNotificationResponse resp = SendNotificationResponse.builder().build();
        when(notificationService.sendNotificationToAllExceptAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(resp);

        mockMvc.perform(post("/notification/admin/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").exists());
    }

    @Test
    void getAllNotificationsAdminSent_returnsList() throws Exception {
        when(notificationService.getAllNotificationsAdminSent()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notification/admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    void getNotificationsBySender_returnsList() throws Exception {
        when(notificationService.getAllNotificationsByManager()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notification/manager"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    void deleteNotification_returnsOk() throws Exception {
        mockMvc.perform(delete("/notification/ND1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Success"));
    }
}
