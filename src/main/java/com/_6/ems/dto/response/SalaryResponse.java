package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SalaryResponse {
    private String id;
    private String personnelCode;
    private String personnelName;
    private Integer month;
    private Integer year;
    private Double grossSalary;
    private Double netSalary;
    private Double totalDeductions;
    private SalaryDetailResponse salaryDetailResponse;
}