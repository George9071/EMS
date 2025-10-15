package com._6.ems.dto.request;

public interface SalaryStatisticsProjection {
    Long getTotalEmployees();
    Double getTotalGross();
    Double getTotalNet();
    Double getTotalDeductions();
    Double getAvgNetSalary();
}
