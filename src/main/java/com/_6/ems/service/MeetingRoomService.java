package com._6.ems.service;

import com._6.ems.dto.request.MeetingRoomRequest;
import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.entity.MeetingRoom;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.MeetingBookingRepository;
import com._6.ems.repository.MeetingRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingBookingRepository meetingBookingRepository;

    public List<MeetingRoomResponse> getAllRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Set<Long> roomIds = rooms.stream()
                .map(MeetingRoom::getId)
                .collect(Collectors.toSet());

        Set<Long> bookedRoomIds = meetingBookingRepository
                .findBookedRoomIds(roomIds, now);

        return rooms.stream()
                .map(room -> convertToDTO(room, bookedRoomIds.contains(room.getId())))
                .toList();
    }

    public MeetingRoomResponse getRoomById(Long id) {
        MeetingRoom meetingRoom = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        boolean isBooked = isRoomBooked(meetingRoom.getId(), now);

        return convertToDTO(meetingRoom, isBooked);
    }

    public List<MeetingRoomResponse> getAvailableRoomsInTimeRange(
            OffsetDateTime startTime, OffsetDateTime endTime) {
        List<MeetingRoom> availableRooms = meetingRoomRepository
                .findAvailableRoomsInTimeRange(startTime, endTime);

        return availableRooms.stream()
                .map(room -> convertToDTO(room, false))
                .toList();
    }

    @Transactional
    public MeetingRoomResponse createRoom(MeetingRoomRequest request) {
        if (meetingRoomRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.MEETING_ROOM_NAME_EXISTED);
        }

        MeetingRoom room = MeetingRoom.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .equipment(request.getEquipment())
                .build();

        MeetingRoom savedRoom = meetingRoomRepository.save(room);
        return convertToDTO(savedRoom, false);
    }

    @Transactional
    public MeetingRoomResponse updateRoom(Long id, MeetingRoomRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        if (!room.getName().equals(request.getName())
                && meetingRoomRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.MEETING_ROOM_NAME_EXISTED);
        }

        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setEquipment(request.getEquipment());

        MeetingRoom updatedRoom = meetingRoomRepository.save(room);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        boolean isBooked = isRoomBooked(updatedRoom.getId(), now);

        return convertToDTO(updatedRoom, isBooked);
    }

    @Transactional
    public void deleteRoom(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (meetingBookingRepository.existsFutureBookingsByRoomId(id, now)) {
            throw new AppException(ErrorCode.MEETING_ROOM_HAS_FUTURE_BOOKINGS);
        }

        meetingRoomRepository.delete(room);
    }


    private boolean isRoomBooked(Long roomId, OffsetDateTime now) {
        return meetingBookingRepository
                .existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(roomId, now, now);
    }

    private MeetingRoomResponse convertToDTO(MeetingRoom room, boolean isBooked) {
        return MeetingRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .equipment(room.getEquipment())
                .isAvailable(!isBooked)
                .build();
    }
}
