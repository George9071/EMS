package com._6.ems.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(min = 2, max = 100, message = "Room name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000, message = "Capacity must not exceed 1000")
    private Integer capacity;

    @Size(max = 500, message = "Equipment description must not exceed 500 characters")
    private String equipment;
}