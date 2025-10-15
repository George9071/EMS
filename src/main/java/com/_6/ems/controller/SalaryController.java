package com._6.ems.controller;
import com._6.ems.dto.request.SalaryUpdateRequest;
import com._6.ems.dto.response.SalaryDetailResponse;
import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.dto.response.SalaryStatisticsResponse;
import com._6.ems.utils.SecurityUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com._6.ems.service.SalaryService;

@RestController
@RequestMapping("/salary")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SalaryController {
    SalaryService salaryService;

    @GetMapping("/my-salaries")
    public ResponseEntity<Page<SalaryResponse>> getMySalaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String personnelCode = SecurityUtil.getCurrentUserCode();
        Pageable pageable = PageRequest.of(page, size);
        Page<SalaryResponse> salaries = salaryService.getSalariesByPersonnelCode(personnelCode, pageable);
        return ResponseEntity.ok(salaries);
    }

    @GetMapping("/personnel/{personnelCode}")
    public ResponseEntity<Page<SalaryResponse>> getSalariesByPersonnel(
            @PathVariable String personnelCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SalaryResponse> salaries = salaryService.getSalariesByPersonnelCode(personnelCode, pageable);
        return ResponseEntity.ok(salaries);
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
