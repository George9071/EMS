package com._6.ems.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceMonthlySummary {
    String code;
    String name;
    int presentDays;
    int lateDays;
    int absentDays;
    double avgHours;
}
