package com._6.ems.dto.response;

import com._6.ems.enums.Gender;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ManagerResponse {
    String code;
    String name;
    Gender gender;
    String email;
    String phone;
    String avatar;
    LocalDate manageDate;
}
