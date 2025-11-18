package com._6.ems.controller;

import java.util.List;

import com._6.ems.dto.request.TaskUpdateRequest;
import com._6.ems.dto.response.FileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.service.TaskService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "Update task")
    @PutMapping("/status")
    public ApiResponse<TaskResponse> updateTask(
            @Parameter(description = "Task ID") @RequestParam String taskId,
            @RequestBody TaskUpdateRequest request) {

        return ApiResponse.<TaskResponse>builder()
                .result(taskService.updateTask(taskId, request))
                .build();
    }

    @Operation(summary = "Upload files to task")
    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> submitTask(
            @RequestBody MultipartFile file,
            @RequestParam String taskId) {

        return ApiResponse.<FileResponse>builder()
                .result(taskService.submitTask(taskId, file))
                .build();
    }

    @Operation(summary = "Get all files that submitted to task")
    @GetMapping("/files/{taskId}")
    public ApiResponse<List<FileResponse>> getFilesOfTask(@PathVariable String taskId) {
        return ApiResponse.<List<FileResponse>>builder()
            .result(taskService.getAllFilesOfTask(taskId))
            .build();
    }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.<Void>builder()
                .message("Delete task success!")
                .build();
    }
}
