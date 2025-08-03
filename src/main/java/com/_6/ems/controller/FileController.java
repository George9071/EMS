package com._6.ems.controller;

import java.util.List;

import com._6.ems.dto.request.FileCreationRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.FileResponse;
import com._6.ems.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {

    FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<FileResponse> uploadFile(
            @RequestBody MultipartFile file,
            @RequestParam String uploaderCode,
            @RequestParam String taskId) {
        FileCreationRequest request = FileCreationRequest.builder()
                .file(file)
                .uploaderCode(uploaderCode)
                .taskId(taskId)
                .build();

        return ApiResponse.<FileResponse>builder()
            .result(fileService.uploadFile(request))
            .build();
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<FileResponse>> getFilesOfTask(@PathVariable String taskId) {
        return ApiResponse.<List<FileResponse>>builder()
            .result(fileService.getAllFilesOfTask(taskId))
            .build();
    }

    @GetMapping("/{fileId}")
    public ApiResponse<FileResponse> getFileByID(@PathVariable String fileId){
        return ApiResponse.<FileResponse>builder()
            .result(fileService.getFileById(fileId))
            .build();
    }
}
