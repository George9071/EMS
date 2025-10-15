package com._6.ems.controller;
import com._6.ems.dto.request.SalaryUpdateRequest;
import com._6.ems.dto.response.*;
import com._6.ems.utils.SecurityUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com._6.ems.service.SalaryService;

@RestController
@RequestMapping("/salary")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SalaryController {
    SalaryService salaryService;

    @GetMapping("/my-salaries")
    public ResponseEntity<ApiResponse<ApiPageResponse<SalaryResponse>>> getMySalaries(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String personnelCode = SecurityUtil.getCurrentUserCode();
        Pageable pageable = PageRequest.of(page, size);

        Page<SalaryResponse> salaries = salaryService.getSalariesByPersonnelCode(personnelCode, month, year, pageable);

        return ResponseEntity.ok(
                ApiPageResponse.success(ApiPageResponse.apiPageResponse(salaries))
        );
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiPageResponse<SalaryResponse>>> getAllSalaries(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<SalaryResponse> salaries = salaryService.getAllSalaries(month, year, pageable);

        return ResponseEntity.ok(
                ApiPageResponse.success(ApiPageResponse.apiPageResponse(salaries))
        );
    }


    @GetMapping("/personnel/{personnelCode}")
    public ResponseEntity<ApiResponse<ApiPageResponse<SalaryResponse>>> getSalariesByPersonnel(
            @PathVariable String personnelCode,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<SalaryResponse> salaries = salaryService.getSalariesByPersonnelCode(personnelCode, month, year, pageable);

        return ResponseEntity.ok(
                ApiPageResponse.success(ApiPageResponse.apiPageResponse(salaries))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryDetailResponse> updateSalary(
            @PathVariable String id,
            @Valid @RequestBody SalaryUpdateRequest request) {
        SalaryDetailResponse salary = salaryService.updateSalary(id, request);
        return ResponseEntity.ok(salary);
    }

    @GetMapping("/statistics")
    public ResponseEntity<SalaryStatisticsResponse> getSalaryStatistics(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        SalaryStatisticsResponse stats = salaryService.getSalaryStatistics(month, year);
        return ResponseEntity.ok(stats);
    }
}
