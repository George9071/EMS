package com._6.ems.controller;

import java.time.LocalDate;
import java.util.List;

import com._6.ems.dto.response.AttendanceMonthlySummary;
import com._6.ems.dto.response.AttendanceOverviewResponse;
import com._6.ems.utils.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.service.AttendanceService;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceController {

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

    @GetMapping("/employee/{code}/{date}")
    public ApiResponse<AttendanceRecordResponse> getAttendanceByDate(
            @PathVariable String code,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        var record = attendanceService.getRecordByDate(code, date);
        return ApiResponse.<AttendanceRecordResponse>builder()
                .result(record)
                .message("Fetched record successfully")
                .build();
    }

    @GetMapping("/today")
    @Operation(summary = "Get detail check-in/check-out list of all employees today")
    public ApiResponse<List<AttendanceRecordResponse>> getTodayAttendanceSummary() {
        var result = attendanceService.getTodayAttendanceSummary();
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched all records for today")
                .build();
    }

    @GetMapping("/month")
    @Operation(summary = "Get detail check-in/check-out list of all employees by month")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year) {

        if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1..12");

        var result = attendanceService.getAllRecordByMonthAndYear(month, year);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched monthly records")
                .build();
    }

    @GetMapping("/summary")
    @Operation
            (summary = "Get all attendance details (present days, late days, absent days, avg hours) of all employees")
    public ApiResponse<List<AttendanceMonthlySummary>> getMonthlySummary(
            @Parameter(description = "Month [1..12]", example = "8") @RequestParam int month,
            @Parameter(description = "Year", example = "2025") @RequestParam int year) {

        var result = attendanceService.getMonthlySummary(month, year);
        return ApiResponse.<List<AttendanceMonthlySummary>>builder()
                .result(result)
                .message("Fetched monthly records")
                .build();
    }

    @GetMapping("/records")
    @Operation(summary = "Get all attendance records by date or interval")
    public List<AttendanceRecordResponse> getAllRecordsByDateOrInterval(
            @Parameter(description = "2025-12-01") @RequestParam(required = true) LocalDate start,
            @Parameter(description = "2025-12-01") @RequestParam(required = true) LocalDate end) {
        return attendanceService.getAllRecordsByDateOrInterval(start, end);
    }

    @GetMapping("/employee/{code}")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCode(
            @PathVariable String code) {
        var result = attendanceService.getAllRecordByEmployeeCode(code);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched records by employee")
                .build();
    }

    @GetMapping("/employee/{code}/range")
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCodeBetween(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (end.isBefore(start)) throw new IllegalArgumentException("end must be >= start");

        var result = attendanceService.getAllRecordByEmployeeCodeInterval(code, start, end);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched records in range")
                .build();
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AttendanceOverviewResponse>> getAttendanceOverview(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = month != null ? month : now.getMonthValue();
            year = year != null ? year : now.getYear();
        }

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AttendanceOverviewResponse>builder()
                            .code(400)
                            .message("Month must be between 1 and 12")
                            .build());
        }

        AttendanceOverviewResponse overview = attendanceService
                .getAttendanceOverview(SecurityUtil.getCurrentUserCode(), month, year);

        return ResponseEntity.ok(ApiResponse.<AttendanceOverviewResponse>builder()
                .code(200)
                .message("Get attendance overview successfully")
                .result(overview)
                .build());
    }
}
