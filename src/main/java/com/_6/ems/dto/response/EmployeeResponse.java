package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class EmployeeResponse {
    String employee_code;
    int department_id;
    int task_completed;
}
