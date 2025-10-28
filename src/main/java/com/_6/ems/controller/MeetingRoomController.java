package com._6.ems.controller;

import com._6.ems.dto.request.MeetingRoomRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.MeetingRoomResponse;
import com._6.ems.service.MeetingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
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
            @RequestParam OffsetDateTime startTime,
            @RequestParam OffsetDateTime endTime) {
        List<MeetingRoomResponse> rooms = roomService
                .getAvailableRoomsInTimeRange(startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @PostMapping("/admin/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MeetingRoomResponse> createRoom(
            @RequestBody @Valid MeetingRoomRequest request) {
        return ApiResponse.<MeetingRoomResponse>builder()
                .result(roomService.createRoom(request))
                .build();
    }

    @PutMapping("/admin/rooms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MeetingRoomResponse> updateRoom(
            @PathVariable Long id,
            @RequestBody @Valid MeetingRoomRequest request) {
        return ApiResponse.<MeetingRoomResponse>builder()
                .result(roomService.updateRoom(id, request))
                .build();
    }

    @DeleteMapping("/admin/rooms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ApiResponse.<Void>builder()
                .message("Meeting room deleted successfully")
                .build();
    }
}