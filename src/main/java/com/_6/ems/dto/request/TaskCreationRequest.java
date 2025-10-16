package com._6.ems.dto.request;

import java.time.OffsetDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskCreationRequest {
    String title;
    String description;
    OffsetDateTime due;
    String project_id;
    String assignee_code;
}
