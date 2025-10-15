package com._6.ems.dto.request;

import com._6.ems.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCreationRequest {
    @Size(min = 6, message = "USERNAME_INVALID, MUST BE AT LEAST 6 CHARACTERS")
    String username;
    @Size(min = 8, message = "INVALID_PASSWORD, MUST BE AT LEAST 8 CHARACTERS")
    String password;

    @Schema(description = "Vai trò của tài khoản", example = "EMPLOYEE",
            allowableValues = {"ADMIN", "EMPLOYEE", "MANAGER"})
    Role role;
}
