package com._6.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;


@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, String> {

    // Find by employee code AND exact work date
    Optional<AttendanceRecord> findByPersonnel_CodeAndDate(String code, LocalDate date);

    // Find all by employee code
    List<AttendanceRecord> findByPersonnel_Code(String code);

    // Find by employee code AND date range
    List<AttendanceRecord> findByPersonnel_CodeAndDateBetween(String code, LocalDate startDate, LocalDate endDate);

    // Find by date range (for all employees)
    List<AttendanceRecord> findByDateBetween(LocalDate startDate, LocalDate endDate);
}