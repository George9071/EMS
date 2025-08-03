package com._6.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Salary;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, String> {

    // Find all salary records by personnel code (owner code)
    List<Salary> findByOwner_Code(String personnelCode);

    // Find salary record for specific personnel by month and year
    Optional<Salary> findByOwner_CodeAndMonthAndYear(String personnelCode, int month, int year);

    // Find all salary records for a given month and year (all personnel)
    List<Salary> findByMonthAndYear(int month, int year);
}
