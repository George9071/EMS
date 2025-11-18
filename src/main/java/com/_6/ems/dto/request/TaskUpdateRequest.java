package com._6.ems.dto.request;

import com._6.ems.enums.ProjectStatus;
import com._6.ems.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskUpdateRequest {
    @Schema(description = "Tên task")
    String title;

    @Schema(description = "Mô tả về task")
    String description;

    @Schema(description = "Deadline")
    OffsetDateTime due;

    @Schema(description = "Trạng thái của task. (CANCELED, CLOSE, COMPLETED, IN_PROGRESS, OVERDUE, PENDING)")
    TaskStatus status;
}
