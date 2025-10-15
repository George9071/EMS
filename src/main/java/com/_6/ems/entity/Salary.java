package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_code", referencedColumnName = "code")
    private Personnel personnel;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;


    @Column(name = "total_work_hours")
    @Builder.Default
    private Double totalWorkHours = 0.0;

    @Column(name = "overtime_hours")
    @Builder.Default
    private Double overtimeHours = 0.0;

    @Column(name = "full_day_work")
    @Builder.Default
    private Integer fullDayWork = 0;

    @Column(name = "half_day_work")
    @Builder.Default
    private Integer halfDayWork = 0;

    @Column(name = "absence_days")
    @Builder.Default
    private Integer absenceDays = 0;

    @Column(name = "late_days")
    @Builder.Default
    private Integer lateDays = 0;

    @Column(name = "not_enough_hour_day")
    @Builder.Default
    private Integer notEnoughHourDays = 0;

    @Column(name = "position_allowance")
    @Builder.Default
    private Double positionAllowance = 0.0;

    @Column(name = "overtime_pay")
    @Builder.Default
    private Double overtimePay = 0.0;


    @Column(name = "social_insurance")
    @Builder.Default
    private Double socialInsurance = 0.0;

    @Column(name = "health_insurance")
    @Builder.Default
    private Double healthInsurance = 0.0;

    @Column(name = "unemployment_insurance")
    @Builder.Default
    private Double unemploymentInsurance = 0.0;

    @Column(name = "personal_income_tax")
    @Builder.Default
    private Double personalIncomeTax = 0.0;

    @Column(name = "penalty")
    @Builder.Default
    private Double penalty = 0.0;


    @Column(name = "gross_salary")
    @Builder.Default
    private Double grossSalary = 0.0;

    @Column(name = "total_deductions")
    @Builder.Default
    private Double totalDeductions = 0.0;

    @Column(name = "net_salary")
    @Builder.Default
    private Double netSalary = 0.0;
}
