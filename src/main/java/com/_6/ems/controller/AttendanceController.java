package com._6.ems.controller;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.service.AttendanceService;

@RestController
@RequestMapping("/attendance")
@Slf4j
public class AttendanceController {

    @Autowired
    AttendanceService attendanceService;

    @PostMapping("/checkIn")
    public ApiResponse<AttendanceRecordResponse> checkIn() {
        AttendanceRecordResponse record = attendanceService.checkIn();
        return ApiResponse.<AttendanceRecordResponse>builder()
            .result(record)
            .message("Check-in successful!, time check-in: " + record.getCheckIn())
            .build();
    }

    @PostMapping("/checkOut")
    public ApiResponse<AttendanceRecordResponse> checkOut() {
        AttendanceRecordResponse record = attendanceService.checkOut();
        return ApiResponse.<AttendanceRecordResponse>builder()
            .result(record)
            .message("Check-out successful!, time check-out: " + record.getCheckOut())
            .build();
    }

    @GetMapping("/{date}")
    public ApiResponse<AttendanceRecordResponse> getAttendanceByDate(
            @RequestParam String code,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AttendanceRecordResponse record = attendanceService.getRecordByDate(code, date);
        return ApiResponse.<AttendanceRecordResponse>builder()
                .result(record)
                .message("Fetch record success!")
                .build();
    }

    @GetMapping("/today")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordToday() {
        log.info("code go here!");

        List<AttendanceRecordResponse> result = attendanceService.getAllRecordToday();

        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetch all records for today successful!")
                .build();
    }

    @GetMapping("/interval")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year) {

        List<AttendanceRecordResponse> result = attendanceService.getAllRecordByMonthAndYear(month, year);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/employee/{employeeCode}")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCode(
            @PathVariable String employeeCode) {
        List<AttendanceRecordResponse> result = attendanceService.getAllRecordByEmployeeCode(employeeCode);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/{employeeCode}/range")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCodeBetween(
            @PathVariable String employeeCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<AttendanceRecordResponse> result =
                attendanceService.getAllRecordByEmployeeCodeBetween(employeeCode, start, end);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .build();
    }
}
