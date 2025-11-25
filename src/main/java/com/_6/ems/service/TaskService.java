package com._6.ems.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.dto.request.TaskUpdateRequest;
import com._6.ems.dto.response.AiReviewResponse;
import com._6.ems.dto.response.FileResponse;
import com._6.ems.entity.File;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.FileMapper;
import com._6.ems.mapper.TaskMapper;
import com._6.ems.repository.FileRepository;
import com._6.ems.utils.CloudinaryUtil;
import com._6.ems.utils.SecurityUtil;
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import static com._6.ems.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskService {

    CloudinaryUtil cloudinaryUtil;
    TaskRepository taskRepository;
    EmployeeRepository employeeRepository;
    ProjectRepository projectRepository;
    FileRepository fileRepository;
    TaskMapper taskMapper;
    FileMapper fileMapper;
    AiService aiService;

    @Transactional
    public TaskResponse createTask(TaskCreationRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(PROJECT_NOT_FOUND));

        Task task = taskMapper.toTask(request);
        task.setProject(project);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignee(null);

        if (request.getAssigneeCode() != null && !request.getAssigneeCode().isEmpty()) {
            Employee assignee = employeeRepository.findById(request.getAssigneeCode())
                    .orElseThrow(() -> new AppException(EMPLOYEE_NOT_FOUND));

            boolean isMember = assignee.getProjects().stream()
                    .anyMatch(assignment -> assignment.getProject().equals(project));

            if (!isMember) throw new AppException(EMPLOYEE_NOT_IN_PROJECT);
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

        if (!isMember) throw new AppException(EMPLOYEE_NOT_IN_PROJECT);

        Employee currentAssignee = task.getAssignee();

        // If the same employee is already assigned, do nothing
        if (currentAssignee != null && currentAssignee.getCode().equals(code)) {
            return taskMapper.toTaskResponse(task);
        }

        task.setAssignee(assignee);
        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(String taskId, TaskUpdateRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(TASK_NOT_FOUND));

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = request.getStatus();
        Employee assignee = task.getAssignee();

        if (newStatus == TaskStatus.COMPLETED && oldStatus != newStatus) {
            if (assignee != null) assignee.setTaskCompleted(assignee.getTaskCompleted() + 1);
        }

        if (newStatus != TaskStatus.COMPLETED && oldStatus == TaskStatus.COMPLETED) {
            if (assignee != null) assignee.setTaskCompleted(Math.max(0, assignee.getTaskCompleted() - 1));
        }

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            task.setLastStatusChange(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        }

        taskMapper.updateTask(request, task);
        taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public FileResponse submitTask(String taskId, MultipartFile file) {
        String code = SecurityUtil.getCurrentUserCode();
        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        cloudinaryUtil.validateFile(file);
        String format = cloudinaryUtil.getUploadFormat(file.getContentType(), file.getOriginalFilename());


        try {
            AiReviewResponse aiReview = aiService.getAiReviewTask(file, task);

            String fileUrl = cloudinaryUtil.uploadToCloudinary(file, format);

            File entity = File.builder()
                    .name(file.getOriginalFilename())
                    .type(file.getContentType())
                    .url(fileUrl)
                    .uploadTime(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                    .uploader(employee)
                    .task(task)
                    .build();

            File saved = fileRepository.save(entity);

            if (task.getFiles() == null) task.setFiles(new ArrayList<>());

            task.getFiles().add(saved);
            taskRepository.save(task);

            FileResponse response = fileMapper.toResponse(saved);
            response.setAiReviewResponse(aiReview);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    public List<FileResponse> getAllFilesOfTask(String taskId) {
        return fileRepository.findByTask_Id(taskId)
                .stream()
                .map(fileMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        taskRepository.delete(task);
    }
}
