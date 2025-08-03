package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectResponse {
    String id;
    String name;
    String description;
    String department_name;
    String status;
    long department_id;
    int participants;
    int max_participants;
}
