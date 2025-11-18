package com._6.ems.controller;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{fileId}")
    public ApiResponse<FileResponse> getFileByID(@PathVariable String fileId){
        return ApiResponse.<FileResponse>builder()
            .result(fileService.getFileById(fileId))
            .build();
    }
}
