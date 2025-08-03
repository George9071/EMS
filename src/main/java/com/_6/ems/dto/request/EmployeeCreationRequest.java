package com._6.ems.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class EmployeeCreationRequest {
    String employee_code;
    int department_id;
}
