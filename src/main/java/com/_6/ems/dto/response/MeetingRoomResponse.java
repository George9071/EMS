package com._6.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private Boolean isAvailable;
    private String location;
    private String equipment;
}