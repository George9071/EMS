package com._6.ems.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AttendanceOverviewResponse {
    private Integer totalDays;
    private Integer presentDays;
    private Integer lateDays;
    private Integer absentDays;
    private Double averageHours;

    private List<AttendanceRecordDTO> records;
}