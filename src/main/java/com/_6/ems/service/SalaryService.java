package com._6.ems.service;

import com._6.ems.dto.request.SalaryStatisticsProjection;
import com._6.ems.dto.request.SalaryUpdateRequest;
import com._6.ems.dto.response.SalaryDetailResponse;
import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.dto.response.SalaryStatisticsResponse;
import com._6.ems.entity.*;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.helper.SalaryHelper;
import com._6.ems.mapper.SalaryMapper;
import com._6.ems.repository.AttendanceRepository;
import com._6.ems.repository.SalaryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalaryService {

    @Value("${salary.standard-work-hours}")
    double standardWorkHours;

    @Value("${salary.overtimeRate}")
    double overtimeRate;

    @Value("${salary.employee.position-allowance}")
    Double positionAllowanceEmployee;

    @Value("${salary.manager.position-allowance}")
    Double positionAllowanceManager;

    final SalaryRepository salaryRepository;
    final AttendanceRepository attendanceRepository;
    final SalaryMapper salaryMapper;
    final SalaryHelper salaryHelper;

    public void createMonthlySalary(Personnel personnel) {
        LocalDate today = LocalDate.now();

        boolean exists = salaryRepository
                .existsByPersonnelAndMonthAndYear(personnel, today.getMonthValue(), today.getYear());
        if (exists) return;

        Salary salary = Salary.builder()
                .personnel(personnel)
                .month(LocalDate.now().getMonthValue())
                .year(LocalDate.now().getYear())
                .build();

        if(personnel.getAccount().getRole() == Role.EMPLOYEE) {
            salary.setPositionAllowance(positionAllowanceEmployee);
        } else if(personnel.getAccount().getRole() == Role.MANAGER) {
            salary.setPositionAllowance(positionAllowanceManager);
        }
        salaryRepository.saveAndFlush(salary);
        calculateSalary(personnel);
    }

    public void calculateSalary(Personnel personnel) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        Salary salary = salaryRepository.findByPersonnelAndMonthAndYear(personnel, month, year)
                .orElseThrow(() -> new AppException(ErrorCode.SALARY_NOT_FOUND));

        List<AttendanceRecord> attendanceRecords = attendanceRepository
                .findByPersonnelCodeAndMonthAndYear(
                        salary.getPersonnel().getCode(),
                        salary.getMonth(),
                        salary.getYear()
                );

        salaryHelper.summarizeMonthlyAttendance(salary, attendanceRecords);

        salaryHelper.calculateInsurance(salary, salary.getPersonnel().getBasicSalary());

        //lương cơ bản theo ngày và theo giờ
        double dailySalary = salary.getPersonnel().getBasicSalary() / salaryHelper.getWorkingDaysInCurrentMonth();
        double hourlySalary = dailySalary / standardWorkHours;

        // 1️⃣ Lương theo công
        double salaryFromWorkDays = salary.getFullDayWork() * dailySalary + salary.getHalfDayWork() * (dailySalary / 2);

        // 2️⃣ Lương tăng ca
        double overtimePay =  salary.getOvertimeHours() * hourlySalary * overtimeRate;
        salary.setOvertimePay(overtimePay);

        // 3️⃣ Tổng thu nhập trước khấu trừ
        double grossSalary = salaryFromWorkDays
                + salary.getPositionAllowance()
                + salary.getPersonnel().getAllowance()
                + salary.getPersonnel().getBonus()
                + overtimePay
                - (salary.getAbsenceDays() * dailySalary)
                - salary.getPersonnel().getKpiPenalty()
                - salary.getPenalty();
        salary.setGrossSalary(grossSalary);

        double personalIncomeTax = salaryHelper.calculatePersonalIncomeTax(salary);
        salary.setPersonalIncomeTax(personalIncomeTax);

        // 4️⃣ Các khoản khấu trừ
        double totalDeductions = salary.getSocialInsurance()
                + salary.getHealthInsurance()
                + salary.getUnemploymentInsurance()
                + personalIncomeTax;
        salary.setTotalDeductions(totalDeductions);

        // 5️⃣ Lương thực nhận
        double netSalary = grossSalary - totalDeductions;
        salary.setNetSalary(netSalary);

        salaryRepository.saveAndFlush(salary);
    }

    @Transactional(readOnly = true)
    public Page<SalaryResponse> getSalariesByPersonnelCode(String personnelCode, Integer month, Integer year, Pageable pageable) {
        Page<Salary> page;

        if (month != null && year != null) {
            page = salaryRepository.findByPersonnelCodeAndMonthAndYearOrderByYearDescMonthDesc(personnelCode, month, year, pageable);
        } else {
            page = salaryRepository.findByPersonnelCodeOrderByYearDescMonthDesc(personnelCode, pageable);
        }

        return page.map(salaryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SalaryResponse> getAllSalaries(Integer month, Integer year, Pageable pageable) {
        Page<Salary> page;

        if (month != null && year != null) {
            page = salaryRepository.findByMonthAndYearOrderByYearDescMonthDesc(month, year, pageable);
        } else {
            page = salaryRepository.findAllByOrderByYearDescMonthDesc(pageable);
        }

        return page.map(salaryMapper::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SalaryDetailResponse updateSalary(String id, SalaryUpdateRequest request) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SALARY_NOT_FOUND));

        salaryMapper.updateEntity(salary, request);

        calculateSalary(salary.getPersonnel());

        Salary updatedSalary = salaryRepository.save(salary);
        return salaryMapper.toDetailResponse(updatedSalary);
    }

    @Transactional(readOnly = true)
    public SalaryStatisticsResponse getSalaryStatistics(Integer month, Integer year) {
        SalaryStatisticsProjection stats = salaryRepository.getStatistics(month, year);

        return SalaryStatisticsResponse.builder()
                .month(month)
                .year(year)
                .totalEmployees(stats.getTotalEmployees())
                .totalGrossSalary(stats.getTotalGross())
                .totalNetSalary(stats.getTotalNet())
                .totalDeductions(stats.getTotalDeductions())
                .averageNetSalary(stats.getAvgNetSalary())
                .build();
    }
}
