package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_attendees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendee_code", nullable = false)
    private String attendeeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private MeetingBooking booking;
}