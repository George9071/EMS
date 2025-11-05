package com._6.ems.service;

import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.exception.AppException;
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

import org.springframework.transaction.annotation.Transactional;

import static com._6.ems.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskService {

    TaskRepository taskRepository;
    EmployeeRepository employeeRepository;
    ProjectRepository projectRepository;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(TaskCreationRequest request) {
        Project project = projectRepository.findById(request.getProject_id())
                .orElseThrow(() -> new AppException(PROJECT_NOT_FOUND));

        Task task = taskMapper.toTask(request);
        task.setProject(project);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignee(null);

        if (request.getAssignee_code() != null && !request.getAssignee_code().isEmpty()) {
            Employee assignee = employeeRepository.findById(request.getAssignee_code())
                    .orElseThrow(() -> new AppException(EMPLOYEE_NOT_FOUND));

            boolean isMember = assignee.getProjects().stream()
                    .anyMatch(assignment -> assignment.getProject().equals(project));

            if (!isMember) throw new AppException(EMPLOYEE_NOT_BELONG_TO_PROJECT);
            task.setAssignee(assignee);
        }

        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    public TaskResponse getTaskById(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(TASK_NOT_FOUND));
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
                .orElseThrow(() -> new AppException(TASK_NOT_FOUND));

        if (task.getStatus() == TaskStatus.COMPLETED) throw new AppException(ASSIGN_COMPLETED_TASK);

        Employee assignee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(EMPLOYEE_NOT_FOUND));

        // Check if the employee is assigned to the task's project
        boolean isMember = assignee.getProjects().stream()
                .anyMatch(project -> project.getProject().equals(task.getProject()));

        if (!isMember) throw new AppException(EMPLOYEE_NOT_BELONG_TO_PROJECT);

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
                .orElseThrow(() -> new AppException(TASK_NOT_FOUND));

        TaskStatus oldStatus = task.getStatus();
        Employee assignee = task.getAssignee();

        if (status == TaskStatus.COMPLETED && oldStatus != status) {
            if (assignee != null) assignee.setTaskCompleted(assignee.getTaskCompleted() + 1);
        }
        else if (status != TaskStatus.COMPLETED && oldStatus == TaskStatus.COMPLETED) {
            if (assignee != null) assignee.setTaskCompleted(Math.max(0, assignee.getTaskCompleted() - 1));
        }

        task.setStatus(status);
        return taskMapper.toTaskResponse(taskRepository.save(task));
    }
}
