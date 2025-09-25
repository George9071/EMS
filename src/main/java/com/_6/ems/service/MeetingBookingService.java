package com._6.ems.service;

import com._6.ems.dto.request.BookingRequest;
import com._6.ems.dto.request.MeetingInvitation;
import com._6.ems.dto.response.MeetingBookingResponse;
import com._6.ems.entity.MeetingAttendee;
import com._6.ems.entity.MeetingBooking;
import com._6.ems.entity.MeetingRoom;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.MeetingBookingRepository;
import com._6.ems.repository.MeetingRoomRepository;
import com._6.ems.utils.PersonnelUtil;
import com._6.ems.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingBookingService {

    private final MeetingBookingRepository bookingRepository;
    private final PersonnelUtil personnelUtil;
    private final MeetingRoomRepository meetingRoomRepository;
    private final EmailService emailService;

    @Transactional
    public MeetingBookingResponse createBooking(BookingRequest request) {
        if (bookingRepository.existsConflictingBooking(
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

        emailService.sendMeetingInvitation(buildMeetingInvitation(booking));

        return convertToDTO(savedBooking);
    }

    public List<MeetingBookingResponse> getMyBookings() {
        String currentUserCode = SecurityUtil.getCurrentUserCode();
        List<MeetingBooking> bookings = bookingRepository.findBookingsByUserCode(currentUserCode);
        return bookings.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public MeetingBookingResponse getBookingById(Long id) {
        MeetingBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_BOOKING_NOT_FOUND));
        return convertToDTO(booking);
    }

    private MeetingInvitation buildMeetingInvitation(MeetingBooking booking) {
        String fullName = personnelUtil.getPersonnelInforByCode(booking.getOrganizerCode()).fullName();
        String departmentName = personnelUtil.getPersonnelInforByCode(booking.getOrganizerCode()).departmentName();
        MeetingRoom meetingRoom = meetingRoomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));
        List<String> recipientEmails = booking.getAttendees()
                .stream()
                .map(a -> personnelUtil.getPersonnelInforByCode(a.getAttendeeCode()).email())
                .toList();
        return MeetingInvitation.builder()
                .meetingTitle(booking.getTitle())
                .meetingDescription(booking.getDescription())
                .organizer(fullName)
                .organizerDepartment(departmentName)
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .roomName(meetingRoom.getName())
                .roomLocation(meetingRoom.getLocation())
                .recipientEmails(recipientEmails)
                .capacity(meetingRoom.getCapacity())
                .build();
    }

    private MeetingBookingResponse convertToDTO(MeetingBooking meetingBooking) {
        String fullName = personnelUtil.getPersonnelInforByCode(meetingBooking.getOrganizerCode()).fullName();
        List<String> attendeeNames = meetingBooking.getAttendees()
                .stream()
                .map(a -> personnelUtil.getPersonnelInforByCode(a.getAttendeeCode()).fullName())
                .toList();
        MeetingRoom meetingRoom = meetingRoomRepository.findById(meetingBooking.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ROOM_NOT_FOUND));
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
}