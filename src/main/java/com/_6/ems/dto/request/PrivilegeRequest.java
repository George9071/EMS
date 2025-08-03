package com._6.ems.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class PrivilegeRequest {
    String name;
    String description;
    Set<String> permissions;
}
