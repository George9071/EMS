package com._6.ems.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com._6.ems.mapper.TaskMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Project;
import com._6.ems.entity.Task;
import com._6.ems.enums.TaskStatus;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ProjectRepository;
import com._6.ems.repository.TaskRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class TaskService {

    TaskRepository taskRepository;
    EmployeeRepository employeeRepository;
    ProjectRepository projectRepository;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(TaskCreationRequest request) {
        Project project = projectRepository.findById(request.getProject_id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        Task task = taskMapper.toTask(request);
        task.setProject(project);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignee(null);

        if (request.getAssignee_code() != null && !request.getAssignee_code().isEmpty()) {
            employeeRepository.findById(request.getAssignee_code())
                    .ifPresent(task::setAssignee);
        }

        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    public TaskResponse getTaskById(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        return taskMapper.toTaskResponse(task);
    }

    public List<TaskResponse> getTasksByProject(String projectId) {
        return taskRepository.findByProject_Id(projectId).stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByAssignee(String employeeCode) {
        return taskRepository.findByAssignee_Code(employeeCode).stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse assignTask(String taskId, String code) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot assign a completed task.");
        }

        Employee assignee = employeeRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        if (!assignee.getProjects().contains(task.getProject())) {
            throw new IllegalArgumentException("Employee is not part of the project.");
        }

        Employee currentAssignee = task.getAssignee();

        // If the same employee is already assigned, do nothing
        if (currentAssignee != null && currentAssignee.getCode().equals(code)) {
            return taskMapper.toTaskResponse(task);
        }

        task.setAssignee(assignee);
        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTaskStatus(String taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (status == TaskStatus.COMPLETED && task.getStatus() != TaskStatus.COMPLETED) {
            if (task.getAssignee() != null) {
                Employee assignee = task.getAssignee();
                assignee.setTaskCompleted(assignee.getTaskCompleted() + 1);
            }
        }

        task.setStatus(status);
        return taskMapper.toTaskResponse(taskRepository.save(task));
    }
}
