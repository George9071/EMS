package com._6.ems.controller;

import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.dto.response.AttendanceStatusResponse;
import com._6.ems.service.AttendanceService;
import com._6.ems.utils.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private org.mockito.MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        AttendanceController controller = new AttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilMock != null) securityUtilMock.close();
    }

    @Test
    void checkIn_returnsRecord() throws Exception {
        AttendanceRecordResponse resp = AttendanceRecordResponse.builder().checkIn("09:00").build();
        when(attendanceService.checkIn()).thenReturn(resp);

        mockMvc.perform(post("/attendance/checkIn").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.checkIn").value("09:00"));
    }

    @Test
    void checkOut_returnsRecord() throws Exception {
        AttendanceRecordResponse resp = AttendanceRecordResponse.builder().checkOut("17:00").build();
        when(attendanceService.checkOut()).thenReturn(resp);

        mockMvc.perform(post("/attendance/checkOut").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.checkOut").value("17:00"));
    }

    @Test
    void getCurrentUserAttendance_callsService() throws Exception {
        securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("P001");
        AttendanceStatusResponse resp = AttendanceStatusResponse.builder().status(com._6.ems.enums.AttendanceStatus.PRESENT).build();
        when(attendanceService.getTodayStatusByPersonnelCode("P001")).thenReturn(resp);

        mockMvc.perform(get("/attendance/today/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PRESENT"));
    }

    @Test
    void getAttendanceByDate_returnsRecord() throws Exception {
        AttendanceRecordResponse resp = AttendanceRecordResponse.builder().checkIn("08:30").build();
        when(attendanceService.getRecordByDate("E1", LocalDate.of(2025,1,1))).thenReturn(resp);

        mockMvc.perform(get("/attendance/employee/E1/2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.checkIn").value("08:30"));
    }

    @Test
    void getAllRecordByEmployeeCode_returnsList() throws Exception {
        when(attendanceService.getAllRecordByEmployeeCode("E1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/attendance/employee/E1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(0));
    }
}
