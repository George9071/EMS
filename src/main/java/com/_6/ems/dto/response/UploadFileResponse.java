package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadFileResponse {
    String fileId;
    String taskId;
    String uploaderCode;
    OffsetDateTime uploadTime;
    String fileUrl;
    String fileName;
    String fileType;

    AiReviewResponse aiReviewResponse;
}
