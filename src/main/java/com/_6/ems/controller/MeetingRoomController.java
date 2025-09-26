package com._6.ems.controller;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingRoomResponse>>> getAllRooms() {
        List<MeetingRoomResponse> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingRoomResponse>> getRoomById(@PathVariable Long id) {
        MeetingRoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping("/available-in-time")
    public ResponseEntity<ApiResponse<List<MeetingRoomResponse>>> getAvailableRoomsInTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        List<MeetingRoomResponse> rooms = roomService
                .getAvailableRoomsInTimeRange(startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}