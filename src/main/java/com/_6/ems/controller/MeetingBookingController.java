package com._6.ems.controller;

import com._6.ems.dto.request.BookingRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.MeetingBookingResponse;
import com._6.ems.service.MeetingBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class MeetingBookingController {

    private final MeetingBookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingBookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        MeetingBookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingBookingResponse>> getBookingById(@PathVariable Long id) {
        MeetingBookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<MeetingBookingResponse>>> getMyBookings() {
        List<MeetingBookingResponse> bookings = bookingService.getMyBookings();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}