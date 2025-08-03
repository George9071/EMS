package com._6.ems.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeInDepartmentResponse {
    String department_name;
    Integer department_id;
    List<EmployeeSimpleResponse> employees;
}
