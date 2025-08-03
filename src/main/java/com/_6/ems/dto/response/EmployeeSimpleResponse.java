package com._6.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class EmployeeSimpleResponse {
    String code;
    String fullName;
}
