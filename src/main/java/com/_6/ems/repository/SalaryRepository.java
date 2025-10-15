package com._6.ems.repository;

import java.time.YearMonth;
import java.util.Optional;

import com._6.ems.dto.request.SalaryStatisticsProjection;
import com._6.ems.entity.Personnel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Salary;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, String> {

    Optional<Salary> findByPersonnelAndMonthAndYear(Personnel personnel, Integer month, Integer year);

    boolean existsByPersonnelAndMonthAndYear(Personnel personnel, Integer month, Integer year);

    Page<Salary> findByPersonnelCodeOrderByYearDescMonthDesc(String personnelCode, Pageable pageable);

    @Query("SELECT s FROM Salary s WHERE " +
            "(s.year > :fromYear OR (s.year = :fromYear AND s.month >= :fromMonth)) AND " +
            "(s.year < :toYear OR (s.year = :toYear AND s.month <= :toMonth)) " +
            "ORDER BY s.year DESC, s.month DESC")
    Page<Salary> findByPeriod(@Param("fromMonth") Integer fromMonth,
                              @Param("fromYear") Integer fromYear,
                              @Param("toMonth") Integer toMonth,
                              @Param("toYear") Integer toYear,
                              Pageable pageable);

    // Thống kê tổng lương theo tháng/năm
    @Query("SELECT " +
            "COUNT(s) as totalEmployees, " +
            "SUM(s.grossSalary) as totalGross, " +
            "SUM(s.netSalary) as totalNet, " +
            "SUM(s.totalDeductions) as totalDeductions, " +
            "AVG(s.netSalary) as avgNetSalary " +
            "FROM Salary s WHERE s.month = :month AND s.year = :year")
    SalaryStatisticsProjection getStatistics(@Param("month") Integer month, @Param("year") Integer year);

    Page<Salary> findByPersonnelCodeAndMonthAndYearOrderByYearDescMonthDesc(
            String personnelCode, Integer month, Integer year, Pageable pageable
    );

    Page<Salary> findAllByOrderByYearDescMonthDesc(Pageable pageable);

    Page<Salary> findByMonthAndYearOrderByYearDescMonthDesc(Integer month, Integer year, Pageable pageable);
}
