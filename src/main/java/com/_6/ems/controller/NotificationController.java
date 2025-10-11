package com._6.ems.controller;

import com._6.ems.dto.request.AdminNotificationRequest;
import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.NotificationResponse;
import com._6.ems.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    NotificationService notificationService;

    @PostMapping("/send")
    public ApiResponse<NotificationResponse> sendNotification(@RequestBody NotificationRequest request) {
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.sendNotification(request))
                .message("Send notification success")
                .build();
    }

    @PostMapping("/admin/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotificationToAllExceptAdmin(
            @RequestBody AdminNotificationRequest request
    ) {
        NotificationResponse response = notificationService.sendNotificationToAllExceptAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
