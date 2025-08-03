package com._6.ems.controller;

import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.service.EmployeeService;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeController {

    EmployeeService employeeService;

    @PostMapping
    public ApiResponse<EmployeeResponse> createEmployee(@RequestBody EmployeeCreationRequest  request){
        return ApiResponse.<EmployeeResponse>builder()
                .result(employeeService.createEmployee(request))
                .message("Add new employee success!")
                .build();
    }

    @GetMapping("/code")
    public ApiResponse<EmployeeResponse> getEmployeeByCode(@RequestParam String code) {
        EmployeeResponse employee = employeeService.getEmployeeByCode(code);
        return ApiResponse.<EmployeeResponse>builder()
            .result(employee)
            .message("Get employee with code: " + code + " success!")
            .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<EmployeeResponse>> getAllEmployee() {
        List<EmployeeResponse> employees = employeeService.getAllEmployee();
        return ApiResponse.<List<EmployeeResponse>>builder()
            .result(employees)
            .message("Get all employee success!")
            .build();
    }
}
