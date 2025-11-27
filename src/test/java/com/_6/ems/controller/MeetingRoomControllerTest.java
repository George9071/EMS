package com._6.ems.controller;

import com._6.ems.dto.request.MeetingRoomRequest;
import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.service.MeetingRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeetingRoomControllerTest {

    @Mock
    private MeetingRoomService roomService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MeetingRoomController controller = new MeetingRoomController(roomService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllRooms_returnsList() throws Exception {
        MeetingRoomResponse r = MeetingRoomResponse.builder().id(1L).name("R1").build();
        when(roomService.getAllRooms()).thenReturn(Arrays.asList(r));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
    void getRoomById_returnsRoom() throws Exception {
        MeetingRoomResponse r = MeetingRoomResponse.builder().id(2L).name("R2").build();
        when(roomService.getRoomById(2L)).thenReturn(r);

        mockMvc.perform(get("/api/rooms/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(2));
    }

    @Test
    void getAvailableRoomsInTimeRange_callsService() throws Exception {
        MeetingRoomResponse r = MeetingRoomResponse.builder().id(3L).name("R3").build();
        when(roomService.getAvailableRoomsInTimeRange(any(), any())).thenReturn(Arrays.asList(r));

        mockMvc.perform(get("/api/rooms/available-in-time")
                        .param("startTime", OffsetDateTime.now().toString())
                        .param("endTime", OffsetDateTime.now().plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
        void createRoom_callsService_andReturns() throws Exception {
        MeetingRoomRequest req = MeetingRoomRequest.builder()
            .name("New")
            .location("Floor 1")
            .capacity(10)
            .isWorking(true)
            .equipment("Projector")
            .build();
        MeetingRoomResponse r = MeetingRoomResponse.builder().id(4L).name("New").build();
        when(roomService.createRoom(any())).thenReturn(r);

        mockMvc.perform(post("/api/rooms/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(4));
        }

    @Test
    void deleteRoom_returnsMessage() throws Exception {
        mockMvc.perform(delete("/api/rooms/admin/rooms/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meeting room deleted successfully"));
    }
}
