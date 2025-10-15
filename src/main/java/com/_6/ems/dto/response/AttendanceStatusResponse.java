package com._6.ems.dto.response;

import com._6.ems.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatusResponse {
    private AttendanceStatus status;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
}