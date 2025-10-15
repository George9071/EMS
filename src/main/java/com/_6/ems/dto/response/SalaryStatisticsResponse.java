package com._6.ems.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStatisticsResponse {
    private Integer month;
    private Integer year;
    private Long totalEmployees;
    private Double totalGrossSalary;
    private Double totalNetSalary;
    private Double totalDeductions;
    private Double averageNetSalary;
}
