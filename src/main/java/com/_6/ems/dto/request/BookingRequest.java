package com._6.ems.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long roomId;

    @NotNull
    private String organizerCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private List<String> attendeeCodes;
}