package com._6.ems.service;

import com._6.ems.dto.request.MeetingRoomRequest;
import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.entity.MeetingRoom;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.MeetingBookingRepository;
import com._6.ems.repository.MeetingRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingRoomServiceTest {

    @Mock
    private MeetingRoomRepository meetingRoomRepository;

    @Mock
    private MeetingBookingRepository meetingBookingRepository;

    @InjectMocks
    private MeetingRoomService meetingRoomService;

    private MeetingRoom meetingRoom;
    private MeetingRoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        meetingRoom = MeetingRoom.builder()
                .id(1L)
                .name("Conference Room A")
                .location("Floor 3")
                .capacity(10)
                .equipment("Projector, Whiteboard")
                .build();

        roomRequest = MeetingRoomRequest.builder()
                .name("Conference Room A")
                .location("Floor 3")
                .capacity(10)
                .equipment("Projector, Whiteboard")
                .build();
    }

    @Test
    void getAllRooms_success() {
        // GIVEN
        MeetingRoom room2 = MeetingRoom.builder()
                .id(2L)
                .name("Conference Room B")
                .location("Floor 4")
                .capacity(15)
                .build();

        List<MeetingRoom> rooms = Arrays.asList(meetingRoom, room2);
        Set<Long> bookedRoomIds = Set.of(1L);

        when(meetingRoomRepository.findAll()).thenReturn(rooms);
        when(meetingBookingRepository.findBookedRoomIds(anySet(), any(OffsetDateTime.class)))
                .thenReturn(bookedRoomIds);

        // WHEN
        List<MeetingRoomResponse> result = meetingRoomService.getAllRooms();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIsAvailable()).isFalse(); // Room 1 is booked
        assertThat(result.get(1).getIsAvailable()).isTrue();  // Room 2 is available
        verify(meetingRoomRepository).findAll();
        verify(meetingBookingRepository).findBookedRoomIds(anySet(), any(OffsetDateTime.class));
    }

    @Test
    void getRoomById_validId_success() {
        // GIVEN
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(meetingRoom));
        when(meetingBookingRepository.existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);

        // WHEN
        MeetingRoomResponse result = meetingRoomService.getRoomById(1L);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Conference Room A");
        assertThat(result.getIsAvailable()).isTrue();
        verify(meetingRoomRepository).findById(1L);
    }

    @Test
    void getRoomById_invalidId_throwsException() {
        // GIVEN
        when(meetingRoomRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> meetingRoomService.getRoomById(999L));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_ROOM_NOT_FOUND);
        verify(meetingRoomRepository).findById(999L);
    }

    @Test
    void getAvailableRoomsInTimeRange_success() {
        // GIVEN
        OffsetDateTime startTime = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        OffsetDateTime endTime = startTime.plusHours(2);

        List<MeetingRoom> availableRooms = Arrays.asList(meetingRoom);

        when(meetingRoomRepository.findAvailableRoomsInTimeRange(startTime, endTime))
                .thenReturn(availableRooms);

        // WHEN
        List<MeetingRoomResponse> result = meetingRoomService.getAvailableRoomsInTimeRange(startTime, endTime);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Conference Room A");
        assertThat(result.get(0).getIsAvailable()).isTrue();
        verify(meetingRoomRepository).findAvailableRoomsInTimeRange(startTime, endTime);
    }

    @Test
    void createRoom_validRequest_success() {
        // GIVEN
        when(meetingRoomRepository.existsByName("Conference Room A")).thenReturn(false);
        when(meetingRoomRepository.save(any(MeetingRoom.class))).thenReturn(meetingRoom);

        // WHEN
        MeetingRoomResponse result = meetingRoomService.createRoom(roomRequest);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Conference Room A");
        assertThat(result.getLocation()).isEqualTo("Floor 3");
        assertThat(result.getCapacity()).isEqualTo(10);
        verify(meetingRoomRepository).existsByName("Conference Room A");
        verify(meetingRoomRepository).save(any(MeetingRoom.class));
    }

    @Test
    void createRoom_roomNameExists_throwsException() {
        // GIVEN
        when(meetingRoomRepository.existsByName("Conference Room A")).thenReturn(true);

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> meetingRoomService.createRoom(roomRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_ROOM_NAME_EXISTED);
        verify(meetingRoomRepository).existsByName("Conference Room A");
        verify(meetingRoomRepository, never()).save(any());
    }

    @Test
    void updateRoom_validRequest_success() {
        // GIVEN
        MeetingRoomRequest updateRequest = MeetingRoomRequest.builder()
                .name("Conference Room A Updated")
                .location("Floor 5")
                .capacity(20)
                .equipment("Projector, Whiteboard, TV")
                .build();

        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(meetingRoom));
        when(meetingRoomRepository.existsByName("Conference Room A Updated")).thenReturn(false);
        when(meetingRoomRepository.save(any(MeetingRoom.class))).thenReturn(meetingRoom);
        when(meetingBookingRepository.existsByRoomIdAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);

        // WHEN
        MeetingRoomResponse result = meetingRoomService.updateRoom(1L, updateRequest);

        // THEN
        assertThat(result).isNotNull();
        verify(meetingRoomRepository).findById(1L);
        verify(meetingRoomRepository).save(any(MeetingRoom.class));
    }

    @Test
    void updateRoom_roomNotFound_throwsException() {
        // GIVEN
        when(meetingRoomRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> meetingRoomService.updateRoom(999L, roomRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_ROOM_NOT_FOUND);
        verify(meetingRoomRepository).findById(999L);
    }

    @Test
    void deleteRoom_noFutureBookings_success() {
        // GIVEN
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(meetingRoom));
        when(meetingBookingRepository.existsFutureBookingsByRoomId(anyLong(), any(OffsetDateTime.class)))
                .thenReturn(false);
        doNothing().when(meetingRoomRepository).delete(meetingRoom);

        // WHEN
        meetingRoomService.deleteRoom(1L);

        // THEN
        verify(meetingRoomRepository).findById(1L);
        verify(meetingBookingRepository).existsFutureBookingsByRoomId(anyLong(), any(OffsetDateTime.class));
        verify(meetingRoomRepository).delete(meetingRoom);
    }

    @Test
    void deleteRoom_hasFutureBookings_throwsException() {
        // GIVEN
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(meetingRoom));
        when(meetingBookingRepository.existsFutureBookingsByRoomId(anyLong(), any(OffsetDateTime.class)))
                .thenReturn(true);

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> meetingRoomService.deleteRoom(1L));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_ROOM_HAS_FUTURE_BOOKINGS);
        verify(meetingRoomRepository).findById(1L);
        verify(meetingRoomRepository, never()).delete(any());
    }
}
