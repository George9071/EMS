package com._6.ems.dto.response;

import com._6.ems.entity.MeetingBooking;
import com._6.ems.entity.MeetingRoom;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoomAvailability {
    private MeetingRoom room;
    private boolean available;
    private List<MeetingBooking> bookings;
}