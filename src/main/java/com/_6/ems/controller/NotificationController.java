package com._6.ems.controller;

import com._6.ems.dto.request.AdminNotificationRequest;
import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.NotificationAdminResponse;
import com._6.ems.dto.response.NotificationManagerResponse;
import com._6.ems.dto.response.SendNotificationResponse;
import com._6.ems.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    NotificationService notificationService;

    @PostMapping("/send")
    public ApiResponse<SendNotificationResponse> sendNotification(@RequestBody NotificationRequest request) {
        return ApiResponse.<SendNotificationResponse>builder()
                .result(notificationService.sendNotification(request))
                .message("Send notification success")
                .build();
    }

    @PostMapping("/admin/send")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotificationToAllExceptAdmin(
            @RequestBody AdminNotificationRequest request
    ) {
        SendNotificationResponse response = notificationService.sendNotificationToAllExceptAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<NotificationAdminResponse>>> getAllNotificationsAdminSent() {
        List<NotificationAdminResponse> response = notificationService.getAllNotificationsAdminSent();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<List<NotificationManagerResponse>>> getNotificationsBySender() {
        List<NotificationManagerResponse> response = notificationService.getAllNotificationsByManager();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable("id") String notificationId) {
        notificationService.deleteNotificationById(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
