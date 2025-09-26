package com._6.ems.service;

import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.entity.MeetingRoom;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.MeetingBookingRepository;
import com._6.ems.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingBookingRepository meetingBookingRepository;

    public List<MeetingRoomResponse> getAllRooms() {
        return meetingRoomRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public MeetingRoomResponse getRoomById(Long id) {
        MeetingRoom meetingRoom = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));
        return convertToDTO(meetingRoom);
    }

    public List<MeetingRoomResponse> getAvailableRoomsInTimeRange(
            LocalDateTime startTime, LocalDateTime endTime) {
        return meetingRoomRepository
                .findAvailableRoomsInTimeRange(startTime, endTime)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public boolean isRoomBooked(Long roomId, LocalDateTime now) {
        return meetingBookingRepository.existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(roomId, now, now);
    }

    private MeetingRoomResponse convertToDTO(MeetingRoom room) {
        boolean booked = isRoomBooked(room.getId(),LocalDateTime.now());
       return MeetingRoomResponse.builder()
               .id(room.getId())
               .name(room.getName())
               .location(room.getLocation())
               .capacity(room.getCapacity())
               .equipment(room.getEquipment())
               .isAvailable(booked)
               .build();
    }
}
