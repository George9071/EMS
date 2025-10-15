package com._6.ems.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSalaryRequest {
    private Double basicSalary;
    private Double bonus;
    private Double allowance;
    private Double kpiPenalty;
}
