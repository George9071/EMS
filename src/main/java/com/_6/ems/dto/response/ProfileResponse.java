package com._6.ems.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProfileResponse {
    long code;
    String last_name;
    String first_name;
    String email;
    String phone;
}