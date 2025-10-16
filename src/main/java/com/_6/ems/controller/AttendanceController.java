package com._6.ems.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com._6.ems.dto.response.*;
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

import com._6.ems.service.AttendanceService;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceController {

    AttendanceService attendanceService;

    @Operation(
        summary = "Check in current user",
        description = "Creates (or updates) today's attendance record for the logged-in user and sets the check-in time."
    )
    @PostMapping("/checkIn")
    public ApiResponse<AttendanceRecordResponse> checkIn() {
        AttendanceRecordResponse attendanceRecord = attendanceService.checkIn();
        return ApiResponse.<AttendanceRecordResponse>builder()
                .result(attendanceRecord)
                .message("Check-in successful!, time check-in: " + attendanceRecord.getCheckIn())
                .build();
    }

    @Operation(
        summary = "Check out current user",
        description = "Sets the check-out time for today's attendance record, " +
                "computes work hours and updates salary counters."
    )
    @PostMapping("/checkOut")
    public ApiResponse<AttendanceRecordResponse> checkOut() {
        AttendanceRecordResponse attendanceRecord = attendanceService.checkOut();
        return ApiResponse.<AttendanceRecordResponse>builder()
                .result(attendanceRecord)
                .message("Check-out successful!, time check-out: " + attendanceRecord.getCheckOut())
                .build();
    }

    @GetMapping("/today/status")
    public ResponseEntity<ApiResponse<AttendanceStatusResponse>> getCurrentUserAttendance() {
        String personnelCode = SecurityUtil.getCurrentUserCode();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getTodayStatusByPersonnelCode(personnelCode
                )));
    }

    @Operation(
        summary = "Get one employee's attendance by date",
        description = "Returns the attendance record for a specific employee and date."
    )
    @GetMapping("/employee/{code}/{date}")
    public ApiResponse<AttendanceRecordResponse> getAttendanceByDate(
            @PathVariable String code,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        var attendanceRecord = attendanceService.getRecordByDate(code, date);
        return ApiResponse.<AttendanceRecordResponse>builder()
                .result(attendanceRecord)
                .message("Fetched record successfully")
                .build();
    }

    @GetMapping("/today")
    @Operation(
        summary = "Get detail check-in/check-out list of all employees today",
        description = "Returns today's attendance records (with check-in/check-out time and duration) for all employees."
    )
    public ApiResponse<List<AttendanceRecordResponse>> getTodayAttendanceSummary() {
        var result = attendanceService.getTodayAttendanceSummary();
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched all records for today")
                .build();
    }

    @GetMapping("/month")
    @Operation(
        summary = "Get detail check-in/check-out list of all employees by month",
        description = "Returns all attendance records for the specified month and year (inclusive)."
    )
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByMonthAndYear(
            @Parameter(description = "1..12") @RequestParam int month,
            @Parameter(example = "2025") @RequestParam int year) {

        if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1..12");

        var result = attendanceService.getAllRecordByMonthAndYear(month, year);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched monthly records")
                .build();
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get attendance summary for all employees",
        description = "For the given month/year, returns per-employee totals: " +
                "present days, late days, absent days, and average work hours."
    )
    public ApiResponse<List<AttendanceMonthlySummary>> getMonthlySummary(
            @Parameter(description = "1..12") @RequestParam int month,
            @Parameter(example = "2025") @RequestParam int year) {

        var result = attendanceService.getMonthlySummary(month, year);
        return ApiResponse.<List<AttendanceMonthlySummary>>builder()
                .result(result)
                .message("Fetched monthly records")
                .build();
    }

    @GetMapping("/records")
    @Operation(
        summary = "Get all attendance records by date or interval",
        description = "Returns all employees' attendance records between the given start and end dates (inclusive). " +
                "If start == end, it's a single day report."
    )
    public List<AttendanceRecordResponse> getAllRecordsByDateOrInterval(
            @Parameter(description = "2025-01-01",required = true) @RequestParam LocalDate start,
            @Parameter(description = "2025-12-31",required = true) @RequestParam LocalDate end) {
        return attendanceService.getAllRecordsByDateOrInterval(start, end);
    }

    @GetMapping("/employee/{code}")
    @Operation(
            summary = "Get all records for one employee",
            description = "Returns all attendance records for the specified employee."
    )
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCode(
            @PathVariable String code) {
        var result = attendanceService.getAllRecordByEmployeeCode(code);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched records by employee")
                .build();
    }

    @GetMapping("/employee/{code}/range")
    @Operation(
            summary = "Get records for one employee between two dates",
            description = "Returns attendance records for the specified employee within the given date range."
    )
    public ApiResponse<List<AttendanceRecordResponse>> getAllRecordByEmployeeCodeBetween(
            @PathVariable String code,
            @Parameter(description = "2025-01-01", required = true) @RequestParam LocalDate start,
            @Parameter(description = "2025-12-31", required = true) @RequestParam LocalDate end) {

        if (end.isBefore(start)) throw new IllegalArgumentException("end must be >= start");

        var result = attendanceService.getAllRecordByEmployeeCodeInterval(code, start, end);
        return ApiResponse.<List<AttendanceRecordResponse>>builder()
                .result(result)
                .message("Fetched records in range")
                .build();
    }

    @GetMapping("/overview")
    @Operation(
        summary = "Get monthly overview for current user",
        description = "Return details (present/late/absent and average hours) for the authenticated user's month/year. " +
                "If month/year are not provided, defaults to the current month/year."
    )
    public ResponseEntity<ApiResponse<AttendanceOverviewResponse>> getAttendanceOverview(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null || year == null) {
            LocalDate now = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
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

//    @GetMapping("/sync")
//    @Operation(
//            summary = "Synchronize data",
//            description = "Synchronize data of a specific month."
//    )
//    public ApiResponse<String> SyncData(
//            @Parameter(description = "2025-01-01", required = true) @RequestParam LocalDate start,
//            @Parameter(description = "2025-12-31", required = true) @RequestParam LocalDate end) {
//
//        int records = attendanceService.syncBetween(start, end);
//
//        return ApiResponse.<String>builder()
//                .code(200)
//                .message("Number of sync records = " + records)
//                .result("Synchronize data successfully")
//                .build();
//    }
}
