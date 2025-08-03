package com._6.ems.dto.response;

import java.time.LocalDateTime;

import com._6.ems.enums.TaskStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskResponse {
    String id;
    String title;
    String description;
    LocalDateTime due;
    TaskStatus status;
    String project_id;
    String assignee_code;
}
