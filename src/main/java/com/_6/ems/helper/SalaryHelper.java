package com._6.ems.helper;

import com._6.ems.entity.AttendanceRecord;
import com._6.ems.entity.Salary;
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalaryHelper {

    private final SalaryRepository salaryRepository;

    public void summarizeMonthlyAttendance(Salary salary, List<AttendanceRecord> attendanceRecords) {
        double totalWorkHours = 0.0;
        double overtimeHours = 0.0;
        double totalMissingHours = 0.0;
        int fullDayWork = 0;
        int halfDayWork = 0;
        int absenceDays = 0;
        int laterDays = 0;
        int notEnoughHourDays = 0;
        int totalLateMinutes = 0;

        if(attendanceRecords.isEmpty()) return;
        log.info("list : {}", attendanceRecords);

        for (AttendanceRecord attendanceRecord : attendanceRecords) {
            if (attendanceRecord.getStatus() == AttendanceStatus.ABSENT) {
                absenceDays++;
                continue;
            }
            if (attendanceRecord.getStatus() == AttendanceStatus.LATE_ARRIVAL){
                laterDays++;
            }

            if (attendanceRecord.getWorkHours() != null) {
                totalWorkHours += attendanceRecord.getWorkHours();
                totalMissingHours += attendanceRecord.getMissingHours();
                totalLateMinutes += attendanceRecord.getLateMinutes();
            }

            switch (attendanceRecord.getType()) {
                case FULL_DAY:
                    fullDayWork++;
                    break;
                case HALF_DAY:
                    halfDayWork++;
                    break;
                case OVERTIME:
                    if (attendanceRecord.getWorkHours() != null) {
                        overtimeHours += attendanceRecord.getWorkHours();
                    }
                    break;
                case NOT_ENOUGH_HOURS:
                    notEnoughHourDays++;
                    break;
            }
        }

        double penaltyForLateMinutes = 0.0;
        if(totalLateMinutes > 30 * getWorkingDaysInCurrentMonth()) { //cho phép muộn 30p mỗi ngày làm việc á
            penaltyForLateMinutes = (((30 * getWorkingDaysInCurrentMonth()) - totalLateMinutes) * 5_000);
        }
        double penaltyForMissingHours = totalMissingHours * 50_000;

        double penalty = penaltyForLateMinutes + penaltyForMissingHours;
        salary.setPenalty(penalty);

        salary.setTotalWorkHours(totalWorkHours);
        salary.setOvertimeHours(overtimeHours);
        salary.setFullDayWork(fullDayWork);
        salary.setHalfDayWork(halfDayWork);
        salary.setAbsenceDays(absenceDays);
        salary.setLateDays(laterDays);
        salary.setNotEnoughHourDays(notEnoughHourDays);

        salaryRepository.saveAndFlush(salary);
    }

    public void calculateInsurance(Salary salary, Double insuranceBaseSalary) {
        if (insuranceBaseSalary != null && insuranceBaseSalary > 0) {
            salary.setSocialInsurance(insuranceBaseSalary * 0.08);
            salary.setHealthInsurance(insuranceBaseSalary * 0.015);
            salary.setUnemploymentInsurance(insuranceBaseSalary * 0.01);
            salaryRepository.saveAndFlush(salary);
        }
    }

    public double calculatePersonalIncomeTax(Salary salary) {
        double taxableIncome = salary.getGrossSalary()
                - salary.getSocialInsurance()
                - salary.getHealthInsurance()
                - salary.getUnemploymentInsurance()
                - 11_000_000;

        double tax = 0;
        if (taxableIncome > 0) {
            if (taxableIncome <= 5_000_000) {
                tax = taxableIncome * 0.05;
            } else if (taxableIncome <= 10_000_000) {
                tax = 5_000_000 * 0.05 + (taxableIncome - 5_000_000) * 0.10;
            } else if (taxableIncome <= 18_000_000) {
                tax = 5_000_000 * 0.05 + 5_000_000 * 0.10 + (taxableIncome - 10_000_000) * 0.15;
            } else if (taxableIncome <= 32_000_000) {
                tax = 5_000_000 * 0.05 + 5_000_000 * 0.10 + 8_000_000 * 0.15 + (taxableIncome - 18_000_000) * 0.20;
            } else if (taxableIncome <= 52_000_000) {
                tax = 5_000_000 * 0.05 + 5_000_000 * 0.10 + 8_000_000 * 0.15 + 14_000_000
                        * 0.20 + (taxableIncome - 32_000_000) * 0.25;
            } else if (taxableIncome <= 80_000_000) {
                tax = 5_000_000 * 0.05 + 5_000_000 * 0.10 + 8_000_000 * 0.15 + 14_000_000
                        * 0.20 + 20_000_000 * 0.25 + (taxableIncome - 52_000_000) * 0.30;
            } else {
                tax = 5_000_000 * 0.05 + 5_000_000 * 0.10 + 8_000_000 * 0.15 + 14_000_000
                        * 0.20 + 20_000_000 * 0.25 + 28_000_000 * 0.30 + (taxableIncome - 80_000_000) * 0.35;
            }
        }

        return tax;
    }

    public int getWorkingDaysInCurrentMonth() {
        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return (int) start.datesUntil(end.plusDays(1))
                .filter(d -> {
                    DayOfWeek dow = d.getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
                })
                .count();
    }
}
