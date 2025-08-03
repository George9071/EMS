package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceRecordResponse {
    String record_id;
    String employee_code;
    String checkIn;
    String checkOut;
    String duration;
}
