package com._6.ems.controller;

import java.util.List;

import com._6.ems.enums.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.service.TaskService;

import lombok.RequiredArgsConstructor;

@Tag(name = "Tasks", description = "Task management APIs")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskController {

    TaskService taskService;

    @Operation(summary = "Create a task")
    @PostMapping()
    public ApiResponse<TaskResponse> createTask(@RequestBody TaskCreationRequest request) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.createTask(request))
                .message("Create new task success!")
                .build();
    }

    @Operation(summary = "Get task by ID")
    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(
            @Parameter(description = "Task ID") @PathVariable String id) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.getTaskById(id))
                .build();
    }

    @Operation(summary = "List tasks by project")
    @GetMapping("/project")
    public ApiResponse<List<TaskResponse>> getTasksByProject(
            @Parameter(description = "Project ID") @RequestParam String projectId) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getTasksByProject(projectId))
                .build();
    }

    @Operation(summary = "List tasks by assignee")
    @GetMapping("/employee")
    public ApiResponse<List<TaskResponse>> getTasksByEmployee(
            @Parameter(description = "Employee code") @RequestParam String code) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getTasksByAssignee(code))
                .build();
    }

    @Operation(summary = "Assign a task to an employee")
    @PutMapping("/assign")
    public ApiResponse<TaskResponse> assignTaskToEmployee(
            @Parameter(description = "Task ID") @RequestParam String taskId,
            @Parameter(description = "Employee code") @RequestParam String employeeCode) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.assignTask(taskId, employeeCode))
                .build();
    }

    @Operation(summary = "Update task status")
    @PutMapping("/status")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @Parameter(description = "Task ID") @RequestParam String taskId,
            @Parameter(description = "Status", example = "COMPLETED") @RequestParam TaskStatus status) {

        return ApiResponse.<TaskResponse>builder()
                .result(taskService.updateTaskStatus(taskId, status))
                .build();
    }
}
