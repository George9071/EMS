package com._6.ems.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.service.EmployeeService;

@Tag(name = "Employees", description = "Employee CRUD APIs")
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeController {

    EmployeeService employeeService;

    @Operation(summary = "Create employee")
    @PostMapping
    public ApiResponse<EmployeeResponse> createEmployee(@RequestBody EmployeeCreationRequest request){
        return ApiResponse.<EmployeeResponse>builder()
                .result(employeeService.createEmployee(request))
                .message("Add new employee success!")
                .build();
    }

    @Operation(summary = "Get employee by code")
    @GetMapping("/code")
    public ApiResponse<EmployeeResponse> getEmployeeByCode(
            @Parameter(description = "Employee code") @RequestParam String code) {
        EmployeeResponse employee = employeeService.getEmployeeByCode(code);
        return ApiResponse.<EmployeeResponse>builder()
            .result(employee)
            .message("Get employee with code: " + code + " success!")
            .build();
    }

    @Operation(summary = "List all employees")
    @GetMapping("/all")
    public ApiResponse<List<EmployeeResponse>> getAllEmployee() {
        List<EmployeeResponse> employees = employeeService.getAllEmployee();
        return ApiResponse.<List<EmployeeResponse>>builder()
            .result(employees)
            .message("Get all employee success!")
            .build();
    }
}
