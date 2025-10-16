package com._6.ems.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeetingInvitation {
    private String meetingTitle;
    private String meetingDescription;
    private String organizer;
    private String organizerDepartment;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String roomName;
    private String roomLocation;
    private Integer capacity;
    private List<String> recipientEmails;
}
