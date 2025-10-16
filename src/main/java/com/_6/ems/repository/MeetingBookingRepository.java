package com._6.ems.repository;

import com._6.ems.entity.MeetingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MeetingBookingRepository extends JpaRepository<MeetingBooking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM MeetingBooking b WHERE b.roomId = :roomId " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsConflictingBooking(
            @Param("roomId") Long roomId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("""
        SELECT DISTINCT mb FROM MeetingBooking mb
        LEFT JOIN mb.attendees a
        WHERE mb.organizerCode = :userCode OR a.attendeeCode = :userCode
    """)
    List<MeetingBooking> findBookingsByUserCode(@Param("userCode") String userCode);

    boolean existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(Long roomId, OffsetDateTime startTime, OffsetDateTime endTime);
}