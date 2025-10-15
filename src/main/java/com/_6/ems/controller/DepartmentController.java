package com._6.ems.controller;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.response.EmployeeInDepartmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.service.DepartmentService;

import java.util.List;

@Tag(name = "Departments", description = "Department management APIs")
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentController {

    DepartmentService departmentService;

    @Operation(
        summary = "Create a department",
        description = "Creates a new department. The department may or may not have a manager at creation time."
    )
    @PostMapping
    public ApiResponse<DepartmentResponse> createDepartment(@RequestBody DepartmentCreationRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.createDepartment(request))
                .message("Add new department success!")
                .build();
    }

    @Operation(
        summary = "Get department by id",
        description = "Returns a department by its  ID."
    )
    @GetMapping
    public ApiResponse<DepartmentResponse> getDepartmentById(
            @Parameter(description = "Department ID") @RequestParam int id) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.getDepartmentById(id))
                .build();
    }

    @GetMapping("/all")
    @Operation(summary = "List all departments")
    public ApiResponse<List<DepartmentResponse>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ApiResponse.<List<DepartmentResponse>>builder()
                .result(departments)
                .build();
    }

    @Operation(summary = "Get employees in a department")
    @GetMapping("/{id}/employees")
    public ApiResponse<EmployeeInDepartmentResponse> getEmployeesInDepartment(
            @Parameter(description = "Department ID") @PathVariable int id) {
        return ApiResponse.<EmployeeInDepartmentResponse>builder()
                .result(departmentService.getAllEmployeesInDepartment(id))
                .build();
    }

    @Operation(
        summary = "Assign manager to department",
        description = "Assigns the manager (by managerId) to the department."
    )
    @PutMapping("/{departmentId}/manager/assign")
    public ApiResponse<DepartmentResponse> assignManager(
            @Parameter(description = "Department ID") @PathVariable int departmentId,
            @Parameter(description = "Manager code") @RequestParam String managerId) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.assignManagerToDepartment(departmentId, managerId))
                .build();
    }


    @Operation(summary = "Remove manager from department")
    @PutMapping("/{departmentId}/manager/remove")
    public ApiResponse<DepartmentResponse> removeManager(
            @Parameter(description = "Department ID") @PathVariable int departmentId) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.removeManagerFromDepartment(departmentId))
                .build();
    }

    @Operation(summary = "Assign employee to department")
    @PutMapping("/{departmentId}/employee/assign")
    public ApiResponse<EmployeeInDepartmentResponse> assignEmployee(
            @Parameter(description = "Department ID") @PathVariable int departmentId,
            @Parameter(description = "Employee code") @RequestParam String employeeCode) {
        return ApiResponse.<EmployeeInDepartmentResponse>builder()
                .result(departmentService.assignEmployeeToDepartment(departmentId, employeeCode))
                .build();
    }

    @Operation(summary = "Remove employee from department")
    @PutMapping("/{departmentId}/employee/remove")
    public ApiResponse<EmployeeInDepartmentResponse> removeEmployee(
            @Parameter(description = "Department ID") @PathVariable int departmentId,
            @Parameter(description = "Employee code") @RequestParam String employeeCode) {
        departmentService.removeEmployeeFromDepartment(departmentId, employeeCode);
        return ApiResponse.<EmployeeInDepartmentResponse>builder()
                .result(departmentService.getAllEmployeesInDepartment(departmentId))
                .build();
    }
}
