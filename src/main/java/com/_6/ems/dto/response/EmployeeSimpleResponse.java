package com._6.ems.dto.response;

import com._6.ems.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeSimpleResponse {
    String code;
    String name;
    Gender gender;
    String email;
    String phone;
    String position;
    String avatar;
}
