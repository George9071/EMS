package com._6.ems.repository;

import com._6.ems.enums.AttendanceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;


@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, String> {

    // Single-day lookup by employee code
    Optional<AttendanceRecord> findByPersonnel_CodeAndDate(String code, LocalDate date);

    // All records for a specific employee
    List<AttendanceRecord> findByPersonnel_Code(String code);

    // Records for an employee in a date range
    List<AttendanceRecord> findByPersonnel_CodeAndDateBetween(String code, LocalDate startDate, LocalDate endDate);

    // Records in a date range (inclusive)
    List<AttendanceRecord> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.personnel.code = :personnelCode " +
            "AND MONTH(a.date) = :month AND YEAR(a.date) = :year " +
            "ORDER BY a.date ASC")
    List<AttendanceRecord> findByPersonnelCodeAndMonthAndYear(
            @Param("personnelCode") String personnelCode,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    /* Advanced */
    long countByPersonnel_CodeAndDateBetweenAndType(String code,
                                                    LocalDate start,
                                                    LocalDate end,
                                                    AttendanceType type);

    // Find the latest "open" record (no checkout yet)
    @Query("""
           select a from AttendanceRecord a
           where a.personnel.code = :code and a.checkOut is null
           order by a.checkIn desc
           """)
    Optional<AttendanceRecord> findLatestOpen(@Param("code") String code);

    // Safe check in. Ensure one record per (person, day)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AttendanceRecord a where a.personnel.code = :code and a.date = :date")
    Optional<AttendanceRecord> findByPersonnel_CodeAndDateForUpdate(@Param("code") String code,
                                                                    @Param("date") LocalDate date);

    // Locks the latest “open” row (where checkOut IS NULL) for that person
    @Lock(LockModeType.PESSIMISTIC_WRITE) // lock the open row for safe checkout
    @Query("""
           select a from AttendanceRecord a
           where a.personnel.code = :code and a.checkOut is null
           order by a.checkIn desc
           """)
    Optional<AttendanceRecord> findOpenForUpdate(@Param("code") String code);
}