package com._6.ems.controller;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.*;
import com._6.ems.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.service.ProjectService;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Tag(name = "Projects", description = "Project management APIs")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {

    ProjectService projectService;

    @Operation(summary = "Create a project")
    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@RequestBody ProjectCreationRequest request) {
        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.createProject(request))
                .message("Create new project success!")
                .build();
    }

    @Operation(summary = "Get project by code")
    @GetMapping("/{code}")
    public ApiResponse<ProjectResponse> getProjectByCode(
            @Parameter(description = "Project ID") @PathVariable String code) {
        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.getProjectById(code))
                .build();
    }

    @Operation(summary = "List projects by status")
    @GetMapping("/status")
    public ApiResponse<List<ProjectResponse>> getProjectsByStatus(
            @Parameter(description = "Project status", example = "IN_PROGRESS") @RequestParam ProjectStatus status) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .result(projectService.getProjectsByStatus(status))
                .build();
    }

    @Operation(summary = "Get employees belong to a project")
    @GetMapping("/{id}/employees")
    public ApiResponse<List<EmployeeSimpleResponse>> getEmployeesInProject(
            @Parameter(description = "Project ID") @PathVariable String id) {
        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
                .result(projectService.getEmployeesInProject(id))
                .build();
    }

    @Operation(summary = "List projects by department")
    @GetMapping("/department")
    public ApiResponse<List<ProjectResponse>> getProjectByDepartment(
            @Parameter(description = "Department ID", example = "1") @RequestParam int deptID) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .result(projectService.getProjectsByDepartment(deptID))
                .message("Get all projects in department with code: " + deptID + " success!")
                .code(200)
                .build();
    }

    @Operation(summary = "Assign employee to project")
    @PostMapping("/{id}/assign")
    public ApiResponse<List<EmployeeSimpleResponse>> assignEmployeeToProject(
            @Parameter(description = "Project ID") @PathVariable String id,
            @Parameter(description = "Employee code") @RequestParam String employeeCode) {

        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
            .result(projectService.assignEmployeeToProject(id, employeeCode))
            .build();
    }

    @Operation(summary = "Remove employee from project")
    @PostMapping("/{id}/remove")
    public ApiResponse<List<EmployeeSimpleResponse>> removeEmployeeFromProject(
            @Parameter(description = "Project ID") @PathVariable String id,
            @Parameter(description = "Employee code") @RequestParam String employeeCode) {

        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
                .result(projectService.removeEmployeeFromProject(id, employeeCode))
                .build();
    }

    @Operation(summary = "Update project status")
    @PutMapping("/{id}/status")
    public ApiResponse<ProjectResponse> updateProjectStatus(
            @Parameter(description = "Project ID") @PathVariable String id,
            @Parameter(description = "Status", example = "COMPLETED") @RequestParam ProjectStatus status) {

        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.updateProjectStatus(id, status))
                .build();
    }

}
