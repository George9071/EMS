package com._6.ems.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public class AttendanceDailySummary {
    List<String> checkedInCodes;
    List<String> checkedOutCodes;
}
