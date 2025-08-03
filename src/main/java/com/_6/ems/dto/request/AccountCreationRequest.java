package com._6.ems.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCreationRequest {
    @Size(min = 6, message = "USERNAME_INVALID")
    String username;
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;
}
