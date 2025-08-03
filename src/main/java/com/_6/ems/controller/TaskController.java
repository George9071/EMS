package com._6.ems.controller;

import java.util.List;

import com._6.ems.enums.TaskStatus;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskController {

    TaskService taskService;

    @PostMapping()
    public ApiResponse<TaskResponse> createTask(@RequestBody TaskCreationRequest request) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.createTask(request))
                .message("Create new task success!")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable String id) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.getTaskById(id))
                .build();
    }

    @GetMapping("/project")
    public ApiResponse<List<TaskResponse>> getTasksByProject(@RequestParam String projectId) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getTasksByProject(projectId))
                .build();
    }

    @GetMapping("/employee")
    public ApiResponse<List<TaskResponse>> getTasksByEmployee(@RequestParam String code) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getTasksByAssignee(code))
                .build();
    }

    @PutMapping("/assign")
    public ApiResponse<TaskResponse> assignTaskToEmployee(
            @RequestParam String taskId,
            @RequestParam String employeeCode) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.assignTask(taskId, employeeCode))
                .build();
    }

    @PutMapping("/status")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @RequestParam String taskId,
            @RequestParam TaskStatus status) {

        return ApiResponse.<TaskResponse>builder()
                .result(taskService.updateTaskStatus(taskId, status))
                .build();
    }
}
