package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SalaryResponse {
    String id;
    String personnel_code;
    int month;
    int year;
    int full_day;
    int half_day;
    int absence;
    double bonus;
    double penalty;
    double real_pay;
}