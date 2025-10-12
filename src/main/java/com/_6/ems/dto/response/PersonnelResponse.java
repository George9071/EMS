package com._6.ems.dto.response;

import com._6.ems.entity.Task;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonnelResponse {
    String code;
    String firstName;
    String lastName;
    String gender;
    String avatarUrl;
    LocalDate dob;
    String email;
    String phoneNumber;
    String city;
    String street;
    String description;
    String skills;
    String position;
    String accountId;
    String role;
    String departmentName;
    List<TaskResponse> tasks;
    Set<PrivilegeResponse> privileges;
}
