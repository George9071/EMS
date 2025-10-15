package com._6.ems.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryUpdateRequest {
    private Double totalWorkHours;
    private Double overtimeHours;
    private Integer fullDayWork;
    private Integer halfDayWork;
    private Integer absenceDays;
    private Integer lateDays;
    private Integer notEnoughHourDays;

    private Double positionAllowance;
    private Double overtimePay;
    private Double penalty;
}
