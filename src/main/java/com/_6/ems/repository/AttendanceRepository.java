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

    /* ========= BASIC LOOKUPS ========= */
    // Non-locking read (just need to check today’s row exists)
    Optional<AttendanceRecord> findByPersonnel_CodeAndDate(String code, LocalDate date);
    // Generic range query
    List<AttendanceRecord> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("""
       select a
       from AttendanceRecord a
       join fetch a.personnel p
       where p.code = :code
       order by a.date desc, a.checkIn desc
    """)
    List<AttendanceRecord> findAllByPersonnelCode(@Param("code") String code);

    @Query("""
       select a
       from AttendanceRecord a
       join fetch a.personnel p
       where p.code = :code
         and a.date between :start and :end
       order by a.date desc, a.checkIn desc
    """)
    List<AttendanceRecord> findAllByPersonnelCodeAndDateBetween(@Param("code") String code,
                                                                @Param("start") LocalDate start,
                                                                @Param("end") LocalDate end);

    /* ========= FETCH-JOIN VARIANTS ========= */

    @Query("""
       select a from AttendanceRecord a
       join fetch a.personnel p
       where a.date between :start and :end
    """)
    List<AttendanceRecord> findAllWithPersonnelByDateBetween(@Param("start") LocalDate start,
                                                             @Param("end") LocalDate end);

    @Query("""
       select a from AttendanceRecord a
       join fetch a.personnel p
       where a.date = :today
    """)
    List<AttendanceRecord> findAllWithPersonnelByDate(@Param("today") LocalDate today);

    /* ========= PESSIMISTIC-LOCKING (use in checkIn / checkOut) ========= */

    // Lock the (person, day) row to prevent duplicate concurrent check-ins/updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from AttendanceRecord a
        join fetch a.personnel p
        where p.code = :code and a.date = :date
        """)
    Optional<AttendanceRecord> findByPersonnel_CodeAndDateForUpdate(@Param("code") String code,
                                                                    @Param("date") LocalDate date);

    // Lock the latest open attendance (no checkout yet) for a person
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from AttendanceRecord a
        join fetch a.personnel p
        where p.code = :code and a.checkOut is null
        order by a.date desc, a.checkIn desc
        """)
    Optional<AttendanceRecord> findOpenForUpdate(@Param("code") String code);
//
    @Query("SELECT a FROM AttendanceRecord a WHERE a.personnel.code = :personnelCode " +
            "AND MONTH(a.date) = :month AND YEAR(a.date) = :year " +
            "ORDER BY a.date ASC")
    List<AttendanceRecord> findByPersonnelCodeAndMonthAndYear(
            @Param("personnelCode") String personnelCode,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}