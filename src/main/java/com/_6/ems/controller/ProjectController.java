package com._6.ems.controller;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.*;
import com._6.ems.enums.ProjectStatus;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.service.ProjectService;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {

    ProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@RequestBody ProjectCreationRequest request) {
        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.createProject(request))
                .message("Create new project success!")
                .build();
    }

    @GetMapping("/{code}")
    public ApiResponse<ProjectResponse> getProjectByCode(@PathVariable String code) {
        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.getProjectById(code))
                .build();
    }

    @GetMapping("/status")
    public ApiResponse<List<ProjectResponse>> getProjectsByStatus(@RequestParam ProjectStatus status) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .result(projectService.getProjectsByStatus(status))
                .build();
    }

    @GetMapping("/{id}/employees")
    public ApiResponse<List<EmployeeSimpleResponse>> getEmployeesInProject(@PathVariable String id) {
        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
                .result(projectService.getEmployeesInProject(id))
                .build();
    }

    @GetMapping("/department")
    public ApiResponse<List<ProjectResponse>> getProjectByDepartment(@RequestParam int deptID) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .result(projectService.getProjectsByDepartment(deptID))
                .message("Get all projects in department with code: " + deptID + " success!")
                .code(200)
                .build();
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<List<EmployeeSimpleResponse>> assignEmployeeToProject(
            @PathVariable String id,
            @RequestParam String employeeCode) {

        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
            .result(projectService.assignEmployeeToProject(id, employeeCode))
            .build();
    }

    @PostMapping("/{id}/remove")
    public ApiResponse<List<EmployeeSimpleResponse>> removeEmployeeFromProject(
            @PathVariable String id,
            @RequestParam String employeeCode) {

        return ApiResponse.<List<EmployeeSimpleResponse>>builder()
                .result(projectService.removeEmployeeFromProject(id, employeeCode))
                .build();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ProjectResponse> updateProjectStatus(
            @PathVariable String id,
            @RequestParam ProjectStatus status) {

        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.updateProjectStatus(id, status))
                .build();
    }

}
