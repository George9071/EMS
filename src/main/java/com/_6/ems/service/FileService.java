package com._6.ems.service;

import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.FileMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.FileResponse;
import com._6.ems.repository.FileRepository;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileService {

    FileRepository fileRepository;
    FileMapper fileMapper;

    public FileResponse getFileById(String fileId) {
        return fileMapper.toResponse(fileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND)));
    }
}
