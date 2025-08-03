package com._6.ems.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.entity.*;
import com._6.ems.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.entity.Salary;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SalaryService {

    @NonFinal
    @Value("${salary.full.employee}")
    protected long FULL_DAY_EMPLOYEE_VALUE;

    @NonFinal
    @Value("${salary.half.employee}")
    protected long HALF_DAY_EMPLOYEE_VALUE;

    @NonFinal
    @Value("${salary.full.manager}")
    protected long FULL_DAY_MANAGER_VALUE;

    @NonFinal
    @Value("${salary.half.manager}")
    protected long HALF_DAY_MANAGER_VALUE;

    @NonFinal
    @Value("${salary.absence}")
    protected long ABSENCE_VALUE;
    
    SalaryRepository salaryRepository;
    PersonnelRepository personnelRepository;
    EmployeeRepository employeeRepository;
    ManagerRepository managerRepository;

    @Transactional
    public SalaryResponse createSalary(String code, Integer month, Integer year) {
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Personnel with code: " + code + " not found!"));

        int finalMonth = getOrDefault(month, LocalDate.now().getMonthValue());
        int finalYear = getOrDefault(year, LocalDate.now().getYear());

        salaryRepository.findByOwner_CodeAndMonthAndYear(code, finalMonth, finalYear)
            .ifPresent(s -> {
                throw new RuntimeException(
                        String.format("Salary already exists for personnel %s in %02d/%d", code, finalMonth, finalYear)
                );
            });

        Salary newRecord = new Salary();
        newRecord.setOwner(personnel);
        newRecord.setMonth(finalMonth);
        newRecord.setYear(finalYear);

        calculate(newRecord);
        return mapToResponse(salaryRepository.save(newRecord));
    }

    @Transactional
    public SalaryResponse getSalaryById(String id) {
        Salary record = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary record not found with ID: " + id));
        calculate(record);
        return mapToResponse(salaryRepository.save(record));
    }

    @Transactional
    public List<SalaryResponse> getAllByPersonnel(String code) {
        return salaryRepository.findByOwner_Code(code).stream()
                .map(this::calculate)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryResponse getDetail(String code, int month, int year) {
        Salary record = salaryRepository.findByOwner_CodeAndMonthAndYear(code, month, year)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found for personnel: " + code +
                        " in month: " + month + ", year: " + year));
        calculate(record);
        return mapToResponse(record);
    }

    @Transactional
    public List<SalaryResponse> getAllRecordByMonthAndYear(int month, int year) {
        return salaryRepository.findByMonthAndYear(month, year).stream()
                .map(this::calculate)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* Helper method */
    private SalaryResponse mapToResponse(Salary record) {
        return SalaryResponse.builder()
                .id(record.getId())
                .personnel_code(record.getOwner().getCode())
                .month(record.getMonth())
                .year(record.getYear())
                .bonus(record.getBonus())
                .penalty(record.getPenalty())
                .full_day(record.getFullWork())
                .half_day(record.getHalfWork())
                .absence(record.getAbsence())
                .real_pay(record.getRealPay())
                .build();
    }

    private Salary calculate(Salary salary){
        int full = salary.getFullWork();
        int half = salary.getHalfWork();
        int absence = salary.getAbsence();
        long fullRate, halfRate;

        String code = salary.getOwner().getCode();
        if (managerRepository.existsById(code)) {
            fullRate = FULL_DAY_MANAGER_VALUE;
            halfRate = HALF_DAY_MANAGER_VALUE;
        } else if (employeeRepository.existsById(code)) {
            fullRate = FULL_DAY_EMPLOYEE_VALUE;
            halfRate = HALF_DAY_EMPLOYEE_VALUE;
        } else {
            throw new IllegalStateException("Personnel not found!");
        }

        double realPay = full * fullRate + half * halfRate - absence * ABSENCE_VALUE;
        salary.setRealPay(realPay);
        return salary;
    }

    private int getOrDefault(Integer value, int defaultValue) {
        return (value != null) ? value : defaultValue;
    }
}
