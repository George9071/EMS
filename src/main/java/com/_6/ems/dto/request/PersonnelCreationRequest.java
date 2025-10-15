package com._6.ems.dto.request;

import com._6.ems.validator.DobConstraint;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonnelCreationRequest {
    String firstName;
    String lastName;

    String gender;

    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob;

    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "INVALID_EMAIL_FORMAT"
    )
    String email;

    @Size(min = 10, max = 10, message = "INVALID_PHONE_NUMBER_LENGTH")
    @Pattern(regexp = "\\d{10}", message = "PHONE_NUMBER_MUST_BE_DIGITS")
    String phoneNumber;

    String city;
    String street;
    String description;
    String skills;
    String position;
    Double basicSalary;


    @Valid
    AccountCreationRequest accountCreationRequest;
}
