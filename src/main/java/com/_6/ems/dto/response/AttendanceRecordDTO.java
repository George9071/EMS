package com._6.ems.dto.response;

import com._6.ems.enums.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {

    @JsonFormat(pattern = "dd MMM yyyy")
    private LocalDate date;

    private String day;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkIn;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkOut;

    private String workHours;

    private AttendanceStatus status;

    private Boolean notEnoughHour;
}