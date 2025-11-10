package com._6.ems.service;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.entity.Employee;
import com._6.ems.entity.EmployeeProject;
import com._6.ems.entity.Project;
import com._6.ems.entity.Task;
import com._6.ems.enums.TaskStatus;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.TaskMapper;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ProjectRepository;
import com._6.ems.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private Employee employee;
    private Task task;
    private TaskCreationRequest creationRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .id("PRJ001")
                .name("Test Project")
                .build();

        employee = Employee.builder()
                .code("EMP001")
                .taskCompleted(5)
                .projects(new ArrayList<>())
                .build();

        task = Task.builder()
                .id("TASK001")
                .title("Test Task")
                .project(project)
                .status(TaskStatus.PENDING)
                .build();

        creationRequest = TaskCreationRequest.builder()
                .title("Test Task")
                .project_id("PRJ001")
                .assignee_code("EMP001")
                .build();

        taskResponse = TaskResponse.builder()
                .id("TASK001")
                .title("Test Task")
                .build();
    }

    @Test
    void createTask_withoutAssignee_success() {
        // GIVEN
        TaskCreationRequest requestWithoutAssignee = TaskCreationRequest.builder()
                .title("Test Task")
                .project_id("PRJ001")
                .build();

        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(taskMapper.toTask(requestWithoutAssignee)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        // WHEN
        TaskResponse result = taskService.createTask(requestWithoutAssignee);

        // THEN
        assertThat(result).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getAssignee()).isNull();
        verify(projectRepository).findById("PRJ001");
        verify(taskRepository).save(task);
    }

    @Test
    void createTask_withAssignee_success() {
        // GIVEN
        EmployeeProject employeeProject = EmployeeProject.builder()
                .employee(employee)
                .project(project)
                .joinedAt(LocalDate.now())
                .build();
        employee.getProjects().add(employeeProject);

        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(taskMapper.toTask(creationRequest)).thenReturn(task);
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        // WHEN
        TaskResponse result = taskService.createTask(creationRequest);

        // THEN
        assertThat(result).isNotNull();
        assertThat(task.getAssignee()).isNotNull();
        verify(projectRepository).findById("PRJ001");
        verify(employeeRepository).findById("EMP001");
        verify(taskRepository).save(task);
    }

    @Test
    void createTask_projectNotFound_throwsException() {
        // GIVEN
        when(projectRepository.findById("PRJ001")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> taskService.createTask(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
        verify(projectRepository).findById("PRJ001");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_employeeNotInProject_throwsException() {
        // GIVEN
        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(taskMapper.toTask(creationRequest)).thenReturn(task);
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> taskService.createTask(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_BELONG_TO_PROJECT);
        verify(employeeRepository).findById("EMP001");
    }

    @Test
    void getTaskById_validId_success() {
        // GIVEN
        when(taskRepository.findById("TASK001")).thenReturn(Optional.of(task));
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        // WHEN
        TaskResponse result = taskService.getTaskById("TASK001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("TASK001");
        verify(taskRepository).findById("TASK001");
    }

    @Test
    void getTaskById_invalidId_throwsException() {
        // GIVEN
        when(taskRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> taskService.getTaskById("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
        verify(taskRepository).findById("INVALID");
    }

    @Test
    void assignTask_success() {
        // GIVEN
        EmployeeProject employeeProject = EmployeeProject.builder()
                .employee(employee)
                .project(project)
                .joinedAt(LocalDate.now())
                .build();
        employee.getProjects().add(employeeProject);

        when(taskRepository.findById("TASK001")).thenReturn(Optional.of(task));
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        // WHEN
        TaskResponse result = taskService.assignTask("TASK001", "EMP001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(task.getAssignee()).isEqualTo(employee);
        verify(taskRepository).findById("TASK001");
        verify(employeeRepository).findById("EMP001");
        verify(taskRepository).save(task);
    }

    @Test
    void assignTask_completedTask_throwsException() {
        // GIVEN
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById("TASK001")).thenReturn(Optional.of(task));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> taskService.assignTask("TASK001", "EMP001"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ASSIGN_COMPLETED_TASK);
        verify(taskRepository).findById("TASK001");
    }

    @Test
    void updateTaskStatus_toCompleted_success() {
        // GIVEN
        task.setAssignee(employee);
        when(taskRepository.findById("TASK001")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        int initialCompletedTasks = employee.getTaskCompleted();

        // WHEN
        TaskResponse result = taskService.updateTaskStatus("TASK001", TaskStatus.COMPLETED);

        // THEN
        assertThat(result).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(employee.getTaskCompleted()).isEqualTo(initialCompletedTasks + 1);
        verify(taskRepository).findById("TASK001");
        verify(taskRepository).save(task);
    }

    @Test
    void updateTaskStatus_fromCompletedToInProgress_success() {
        // GIVEN
        task.setStatus(TaskStatus.COMPLETED);
        task.setAssignee(employee);
        when(taskRepository.findById("TASK001")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        int initialCompletedTasks = employee.getTaskCompleted();

        // WHEN
        TaskResponse result = taskService.updateTaskStatus("TASK001", TaskStatus.IN_PROGRESS);

        // THEN
        assertThat(result).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(employee.getTaskCompleted()).isEqualTo(initialCompletedTasks - 1);
        verify(taskRepository).findById("TASK001");
        verify(taskRepository).save(task);
    }

    @Test
    void getTasksByProject_success() {
        // GIVEN
        Task task2 = Task.builder().id("TASK002").title("Task 2").project(project).build();
        List<Task> tasks = Arrays.asList(task, task2);
        TaskResponse response2 = TaskResponse.builder().id("TASK002").build();

        when(taskRepository.findByProject_Id("PRJ001")).thenReturn(tasks);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);
        when(taskMapper.toTaskResponse(task2)).thenReturn(response2);

        // WHEN
        List<TaskResponse> result = taskService.getTasksByProject("PRJ001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(taskRepository).findByProject_Id("PRJ001");
    }

    @Test
    void getTasksByAssignee_success() {
        // GIVEN
        List<Task> tasks = Arrays.asList(task);
        when(taskRepository.findByAssignee_Code("EMP001")).thenReturn(tasks);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse);

        // WHEN
        List<TaskResponse> result = taskService.getTasksByAssignee("EMP001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(taskRepository).findByAssignee_Code("EMP001");
    }
}
