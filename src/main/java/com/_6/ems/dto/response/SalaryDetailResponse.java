package com._6.ems.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryDetailResponse {
    private String id;

    // Thông tin nhân viên
    private String personnelCode;
    private String personnelName;
    private String position;

    // Kỳ lương
    private Integer month;
    private Integer year;

    // Giờ công
    private Double totalWorkHours;
    private Double overtimeHours;
    private Integer fullDayWork;
    private Integer halfDayWork;
    private Integer absenceDays;
    private Integer lateDays;
    private Integer notEnoughHourDays;

    // Thu nhập
    private Double positionAllowance;
    private Double overtimePay;

    // Các khoản khấu trừ
    private Double socialInsurance;
    private Double healthInsurance;
    private Double unemploymentInsurance;
    private Double personalIncomeTax;
    private Double penalty;

    // Tổng kết
    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;
}