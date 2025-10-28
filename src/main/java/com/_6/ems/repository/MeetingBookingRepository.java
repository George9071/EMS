package com._6.ems.repository;

import com._6.ems.entity.MeetingBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MeetingBookingRepository extends JpaRepository<MeetingBooking, Long> {

    boolean existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(Long roomId, OffsetDateTime startTime, OffsetDateTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM MeetingBooking b " +
            "WHERE b.roomId = :roomId " +
            "AND b.startTime < :endTime " +
            "AND b.endTime > :startTime")
    boolean existsConflictingBookingWithLock(@Param("roomId") Long roomId,
                                             @Param("startTime") OffsetDateTime startTime,
                                             @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT DISTINCT b FROM MeetingBooking b " +
            "LEFT JOIN FETCH b.attendees a " +
            "WHERE b.organizerCode = :userCode " +
            "OR a.attendeeCode = :userCode " +
            "ORDER BY b.startTime DESC")
    List<MeetingBooking> findBookingsByUserCodeWithAttendees(@Param("userCode") String userCode);

    @Query("SELECT b FROM MeetingBooking b " +
            "LEFT JOIN FETCH b.attendees " +
            "WHERE b.id = :id")
    Optional<MeetingBooking> findByIdWithAttendees(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM MeetingBooking b " +
            "LEFT JOIN FETCH b.attendees " +
            "ORDER BY b.startTime DESC")
    List<MeetingBooking> findAllWithAttendees();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM MeetingBooking b " +
            "WHERE b.id != :excludeId " +
            "AND b.roomId = :roomId " +
            "AND b.startTime < :endTime " +
            "AND b.endTime > :startTime")
    boolean existsConflictingBookingExcept(
            @Param("excludeId") Long excludeId,
            @Param("roomId") Long roomId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT DISTINCT b.roomId FROM MeetingBooking b " +
            "WHERE b.roomId IN :roomIds " +
            "AND b.startTime <= :now " +
            "AND b.endTime > :now")
    Set<Long> findBookedRoomIds(
            @Param("roomIds") Set<Long> roomIds,
            @Param("now") OffsetDateTime now);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM MeetingBooking b " +
            "WHERE b.roomId = :roomId " +
            "AND b.endTime > :now")
    boolean existsFutureBookingsByRoomId(
            @Param("roomId") Long roomId,
            @Param("now") OffsetDateTime now);
}