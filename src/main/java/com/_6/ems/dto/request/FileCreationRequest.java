package com._6.ems.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileCreationRequest {
    String fileName;
    MultipartFile file;
    String uploaderCode;
    String taskId;
}
