package com._6.ems.controller;
import com._6.ems.mapper.SalaryMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;

import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.service.SalaryService;

@RestController
@RequestMapping("/salary")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SalaryController {
    SalaryService salaryService;
    SalaryMapper salaryMapper;

    @PostMapping()
    public ApiResponse<SalaryResponse> createRecord(
            @RequestParam String code,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        return ApiResponse.<SalaryResponse>builder()
                .result(salaryMapper.toSalaryResponse(salaryService.createSalary(code, month, year)))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SalaryResponse> getRecord(@PathVariable String id) {
        return ApiResponse.<SalaryResponse>builder()
                .result(salaryService.getSalaryById(id))
                .build();
    }

    @GetMapping("/employee")
    public ApiResponse<List<SalaryResponse>> getAllRecordOfPersonnel(@RequestParam String code) {
        return ApiResponse.<List<SalaryResponse>>builder()
                .result(salaryService.getAllByPersonnel(code))
                .build();
    }

    @GetMapping("/detail")
    public ApiResponse<SalaryResponse> getDetailRecord(
            @RequestParam String code,
            @RequestParam int month,
            @RequestParam int year) {

        return ApiResponse.<SalaryResponse>builder()
                .result(salaryService.getDetail(code, month, year))
                .build();
    }

    @GetMapping("/interval")
    public ApiResponse<List<SalaryResponse>> getAllRecordOfEmployee(@RequestParam int month, @RequestParam int year) {
        return ApiResponse.<List<SalaryResponse>>builder()
                .result(salaryService.getAllRecordByMonthAndYear(month, year))
                .build();
    }
}
