package com._6.ems.service;

import com._6.ems.dto.request.BookingRequest;
import com._6.ems.dto.request.MeetingInvitation;
import com._6.ems.dto.response.MeetingBookingResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.MeetingAttendee;
import com._6.ems.entity.MeetingBooking;
import com._6.ems.entity.MeetingRoom;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.record.PersonnelInfo;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.MeetingBookingRepository;
import com._6.ems.repository.MeetingRoomRepository;
import com._6.ems.utils.PersonnelUtil;
import com._6.ems.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingBookingService {

    private final MeetingBookingRepository bookingRepository;
    private final PersonnelUtil personnelUtil;
    private final MeetingRoomRepository meetingRoomRepository;
    private final EmailService emailService;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public MeetingBookingResponse createBooking(BookingRequest request) {
        if (bookingRepository.existsConflictingBookingWithLock(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime())) {
            throw new AppException(ErrorCode.MEETING_ROOM_CONFLICT);
        }

        MeetingBooking booking = MeetingBooking.builder()
                .roomId(request.getRoomId())
                .organizerCode(request.getOrganizerCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        List<MeetingAttendee> attendees = request.getAttendeeCodes().stream()
                .map(code -> MeetingAttendee.builder()
                        .attendeeCode(code)
                        .booking(booking)
                        .build())
                .toList();

        booking.setAttendees(attendees);

        MeetingBooking savedBooking = bookingRepository.save(booking);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendMeetingInvitation(buildMeetingInvitation(savedBooking));
            } catch (Exception e) {
                log.error("Failed to send meeting invitation email for booking: {}", savedBooking.getId(), e);
            }
        });

        return convertToDTO(savedBooking);
    }

    public List<MeetingBookingResponse> getMyBookings() {
        String currentUserCode = SecurityUtil.getCurrentUserCode();

        List<MeetingBooking> bookings = bookingRepository.findBookingsByUserCodeWithAttendees(currentUserCode);

        Set<Long> roomIds = bookings.stream()
                .map(MeetingBooking::getRoomId)
                .collect(Collectors.toSet());

        Set<String> allCodes = bookings.stream()
                .flatMap(b -> Stream.concat(
                        Stream.of(b.getOrganizerCode()),
                        b.getAttendees().stream().map(MeetingAttendee::getAttendeeCode)
                ))
                .collect(Collectors.toSet());

        Map<String, PersonnelInfo> personnelMap = personnelUtil.getPersonnelInfoByCodes(allCodes);
        Map<Long, MeetingRoom> roomMap = meetingRoomRepository.findAllById(roomIds)
                .stream()
                .collect(Collectors.toMap(MeetingRoom::getId, Function.identity()));

        return bookings.stream()
                .map(booking -> convertToDTO(booking, personnelMap, roomMap))
                .toList();
    }

    public MeetingBookingResponse getBookingById(Long id) {
        MeetingBooking booking = bookingRepository.findByIdWithAttendees(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_BOOKING_NOT_FOUND));

        Set<String> allCodes = Stream.concat(
                Stream.of(booking.getOrganizerCode()),
                booking.getAttendees().stream().map(MeetingAttendee::getAttendeeCode)
        ).collect(Collectors.toSet());

        Map<String, PersonnelInfo> personnelMap = personnelUtil.getPersonnelInfoByCodes(allCodes);
        MeetingRoom room = meetingRoomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        return convertToDTO(booking, personnelMap, Map.of(room.getId(), room));
    }

    public List<MeetingBookingResponse> getAllBookings() {
        List<MeetingBooking> bookings = bookingRepository.findAllWithAttendees();

        Set<Long> roomIds = bookings.stream()
                .map(MeetingBooking::getRoomId)
                .collect(Collectors.toSet());

        Set<String> allCodes = bookings.stream()
                .flatMap(b -> Stream.concat(
                        Stream.of(b.getOrganizerCode()),
                        b.getAttendees().stream().map(MeetingAttendee::getAttendeeCode)
                ))
                .collect(Collectors.toSet());

        Map<String, PersonnelInfo> personnelMap = personnelUtil.getPersonnelInfoByCodes(allCodes);
        Map<Long, MeetingRoom> roomMap = meetingRoomRepository.findAllById(roomIds)
                .stream()
                .collect(Collectors.toMap(MeetingRoom::getId, Function.identity()));

        return bookings.stream()
                .map(booking -> convertToDTO(booking, personnelMap, roomMap))
                .toList();
    }

    @Transactional
    public MeetingBookingResponse updateBooking(Long id, BookingRequest request) {
        MeetingBooking booking = bookingRepository.findByIdWithAttendees(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_BOOKING_NOT_FOUND));

        String currentUserCode = SecurityUtil.getCurrentUserCode();

        if (!booking.getOrganizerCode().equals(currentUserCode)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_UPDATE_BOOKING);
        }

        boolean isRoomOrTimeChanged = !booking.getRoomId().equals(request.getRoomId())
                || !booking.getStartTime().equals(request.getStartTime())
                || !booking.getEndTime().equals(request.getEndTime());

        if (isRoomOrTimeChanged) {
            if (bookingRepository.existsConflictingBookingExcept(
                    id,
                    request.getRoomId(),
                    request.getStartTime(),
                    request.getEndTime())) {
                throw new AppException(ErrorCode.MEETING_ROOM_CONFLICT);
            }
        }

        booking.setRoomId(request.getRoomId());
        booking.setTitle(request.getTitle());
        booking.setDescription(request.getDescription());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        booking.getAttendees().clear();
        List<MeetingAttendee> newAttendees = request.getAttendeeCodes().stream()
                .map(code -> MeetingAttendee.builder()
                        .attendeeCode(code)
                        .booking(booking)
                        .build())
                .toList();
        booking.getAttendees().addAll(newAttendees);

        MeetingBooking updatedBooking = bookingRepository.save(booking);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendMeetingUpdateNotification(buildMeetingInvitation(updatedBooking));
            } catch (Exception e) {
                log.error("Failed to send meeting update notification for booking: {}", updatedBooking.getId(), e);
            }
        });

        return convertToDTO(updatedBooking);
    }

    @Transactional
    public void deleteBooking(Long id, String cancelReason) {
        MeetingBooking booking = bookingRepository.findByIdWithAttendees(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_BOOKING_NOT_FOUND));

        String currentUserCode = SecurityUtil.getCurrentUserCode();

        if (!booking.getOrganizerCode().equals(currentUserCode)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_DELETE_BOOKING);
        }

        MeetingInvitation cancellationInfo = buildMeetingInvitation(booking);

        bookingRepository.delete(booking);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendMeetingCancellationNotification(cancellationInfo, cancelReason);
            } catch (Exception e) {
                log.error("Failed to send meeting cancellation notification for booking: {}", id, e);
            }
        });
    }

    private MeetingInvitation buildMeetingInvitation(MeetingBooking booking) {
        Set<String> allCodes = Stream.concat(
                Stream.of(booking.getOrganizerCode()),
                booking.getAttendees().stream().map(MeetingAttendee::getAttendeeCode)
        ).collect(Collectors.toSet());

        Map<String, PersonnelInfo> personnelMap = personnelUtil.getPersonnelInfoByCodes(allCodes);

        Department department = departmentRepository.findDepartmentByPersonnelCode(booking.getOrganizerCode());

        MeetingRoom meetingRoom = meetingRoomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        List<String> recipientEmails = booking.getAttendees()
                .stream()
                .map(a -> personnelMap.get(a.getAttendeeCode()).email())
                .filter(Objects::nonNull)
                .toList();

        return MeetingInvitation.builder()
                .meetingTitle(booking.getTitle())
                .meetingDescription(booking.getDescription())
                .organizer(personnelMap.get(booking.getOrganizerCode()).fullName())
                .organizerDepartment(department.getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .roomName(meetingRoom.getName())
                .roomLocation(meetingRoom.getLocation())
                .recipientEmails(recipientEmails)
                .capacity(meetingRoom.getCapacity())
                .build();
    }

    private MeetingBookingResponse convertToDTO(MeetingBooking meetingBooking,
                                                Map<String, PersonnelInfo> personnelMap,
                                                Map<Long, MeetingRoom> roomMap) {
        String fullName = personnelMap.get(meetingBooking.getOrganizerCode()).fullName();

        List<String> attendeeNames = meetingBooking.getAttendees()
                .stream()
                .map(a -> personnelMap.get(a.getAttendeeCode()).fullName())
                .filter(Objects::nonNull)
                .toList();

        MeetingRoom meetingRoom = roomMap.get(meetingBooking.getRoomId());
        if (meetingRoom == null) {
            throw new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }

        return MeetingBookingResponse.builder()
                .id(meetingBooking.getId())
                .roomName(meetingRoom.getName())
                .title(meetingBooking.getTitle())
                .description(meetingBooking.getDescription())
                .organizerName(fullName)
                .attendeeNames(attendeeNames)
                .startTime(meetingBooking.getStartTime())
                .endTime(meetingBooking.getEndTime())
                .createdAt(meetingBooking.getCreatedAt())
                .build();
    }

    private MeetingBookingResponse convertToDTO(MeetingBooking meetingBooking) {
        Set<String> allCodes = Stream.concat(
                Stream.of(meetingBooking.getOrganizerCode()),
                meetingBooking.getAttendees().stream().map(MeetingAttendee::getAttendeeCode)
        ).collect(Collectors.toSet());

        Map<String, PersonnelInfo> personnelMap = personnelUtil.getPersonnelInfoByCodes(allCodes);
        MeetingRoom room = meetingRoomRepository.findById(meetingBooking.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        return convertToDTO(meetingBooking, personnelMap, Map.of(room.getId(), room));
    }
}