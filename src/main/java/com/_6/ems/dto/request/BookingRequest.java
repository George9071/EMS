package com._6.ems.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(type = "string", example = "2025-10-05T09:30:00")
    @NotNull
    @Future
    private LocalDateTime startTime;

    @Schema(type = "string", example = "2025-10-05T09:30:00")
    @NotNull
    private LocalDateTime endTime;

    private List<String> attendeeCodes;
}