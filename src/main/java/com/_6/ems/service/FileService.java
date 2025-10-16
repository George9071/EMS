package com._6.ems.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.dto.request.FileCreationRequest;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.FileMapper;
import com._6.ems.utils.CloudinaryUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com._6.ems.dto.response.FileResponse;
import com._6.ems.entity.Employee;
import com._6.ems.entity.File;
import com._6.ems.entity.Task;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.FileRepository;
import com._6.ems.repository.TaskRepository;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileService {

    CloudinaryUtil cloudinaryUtil;
    FileRepository fileRepository;
    EmployeeRepository employeeRepository;
    TaskRepository taskRepository;
    FileMapper fileMapper;


    public FileResponse uploadFile(FileCreationRequest request) {
        MultipartFile file = request.getFile();

        Employee employee = employeeRepository.findById(request.getUploaderCode())
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        cloudinaryUtil.validateFile(file);

        String format = cloudinaryUtil.getUploadFormat(file.getContentType(), file.getOriginalFilename());

        try {
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

            if (task.getFiles() == null) {
                task.setFiles(new ArrayList<>());
            }

            task.getFiles().add(saved);
            taskRepository.save(task);

            return fileMapper.toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    public List<FileResponse> getAllFilesOfTask(String taskId) {
        return fileRepository.findByTask_Id(taskId)
                .stream()
                .map(fileMapper::toResponse)
                .collect(Collectors.toList());
    }

    public FileResponse getFileById(String fileId) {
        return fileMapper.toResponse(fileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND)));
    }
}
