package com._6.ems.repository;

import com._6.ems.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    @Query("SELECT r FROM MeetingRoom r WHERE r.id NOT IN " +
            "(SELECT b.roomId FROM MeetingBooking b WHERE " +
            "(b.startTime < :endTime AND b.endTime > :startTime))")
    List<MeetingRoom> findAvailableRoomsInTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    boolean existsByName(String name);
}