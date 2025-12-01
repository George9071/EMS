package com._6.ems.service;

import com._6.ems.dto.response.FileResponse;
import com._6.ems.entity.File;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.FileMapper;
import com._6.ems.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileService fileService;

    private File fileEntity;
    private FileResponse fileResponse;

    @BeforeEach
    void setUp() {
        fileEntity = File.builder()
                .id("F1")
                .name("document.pdf")
                .url("http://example.com/document.pdf")
                .uploadTime(OffsetDateTime.now())
                .build();

        fileResponse = FileResponse.builder()
                .fileId("F1")
                .fileName("document.pdf")
                .fileUrl("http://example.com/document.pdf")
                .uploadTime(fileEntity.getUploadTime())
                .build();
    }

    @Test
    void getFileById_existing_returnsResponse() {
        // GIVEN
        when(fileRepository.findById("F1")).thenReturn(Optional.of(fileEntity));
        when(fileMapper.toFileResponse(fileEntity)).thenReturn(fileResponse);

        // WHEN
        FileResponse result = fileService.getFileById("F1");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo("F1");
        assertThat(result.getFileName()).isEqualTo("document.pdf");
        verify(fileRepository).findById("F1");
        verify(fileMapper).toFileResponse(fileEntity);
    }

    @Test
    void getFileById_notFound_throwsAppException() {
        // GIVEN
        when(fileRepository.findById(anyString())).thenReturn(Optional.empty());

        // WHEN / THEN
        AppException ex = assertThrows(AppException.class, () -> fileService.getFileById("MISSING"));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
        verify(fileRepository).findById("MISSING");
    }

    @Test
    void getFileById_invokesMapperWithEntity() {
        // GIVEN
        when(fileRepository.findById("F1")).thenReturn(Optional.of(fileEntity));
        when(fileMapper.toFileResponse(fileEntity)).thenReturn(fileResponse);

        // WHEN
        fileService.getFileById("F1");

        // THEN
        verify(fileMapper, times(1)).toFileResponse(fileEntity);
    }

    @Test
    void getFileById_callsRepositoryWithExactId() {
        // GIVEN
        when(fileRepository.findById("F1")).thenReturn(Optional.of(fileEntity));
        when(fileMapper.toFileResponse(fileEntity)).thenReturn(fileResponse);

        // WHEN
        fileService.getFileById("F1");

        // THEN
        verify(fileRepository, times(1)).findById("F1");
    }

    @Test
    void getFileById_nullId_throwsAppException() {
        // GIVEN
        when(fileRepository.findById(null)).thenReturn(Optional.empty());

        // WHEN / THEN
        AppException ex = assertThrows(AppException.class, () -> fileService.getFileById(null));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
        verify(fileRepository).findById(null);
    }
}
