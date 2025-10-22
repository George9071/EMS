package com._6.ems.controller;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.*;
import com._6.ems.service.PersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Personnel", description = "Personnel profile APIs")
@Slf4j
@RestController
@RequestMapping("/personnels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonnelController {
    PersonnelService personnelService;

    @Operation(summary = "Get my profile")
    @GetMapping("/myInfo")
    ApiResponse<PersonnelResponse> getMyInfo(){
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.getMyInfo())
                .build();
    }

    @Operation(summary = "Get my notifications")
    @GetMapping("/notifications")
    ApiResponse<List<NotificationReceiverResponse>> getMyNotification(){
        return ApiResponse.<List<NotificationReceiverResponse>>builder()
                .result(personnelService.getMyNoti())
                .build();
    }

    @Operation(summary = "List all personnel details")
    @GetMapping("/all")
    public ApiResponse<List<PersonnelResponse>> getAllPersonnel() {
        List<PersonnelResponse> personnel = personnelService.getAllPersonnel();
        return ApiResponse.<List<PersonnelResponse>>builder()
                .result(personnel)
                .build();
    }

    @Operation(summary = "Get personnel data by code")
    @GetMapping("/{code}")
    public ApiResponse<PersonnelResponse> getPersonnelByCode(
            @Parameter(description = "Personnel code") @PathVariable String code) {
        PersonnelResponse personnel = personnelService.getPersonnelByCode(code);
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnel)
                .build();
    }

    @Operation(summary = "Mark notification as read")
    @PostMapping("/notifications/{notificationId}")
    public ApiResponse<NotificationReceiverResponse> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable String notificationId) {
        return ApiResponse.<NotificationReceiverResponse>builder()
                .result(personnelService.markAsRead(notificationId))
                .build();
    }

    @Operation(summary = "Create personnel")
    @PostMapping
    ApiResponse<PersonnelResponse> createPersonnel(@RequestBody @Valid PersonnelCreationRequest request){
        ApiResponse<PersonnelResponse> apiResponse = new ApiResponse<>();

        apiResponse.setResult(personnelService.createPersonnel(request));

        return apiResponse;
    }

    @Operation(summary = "Update personnel (partial)")
    @PatchMapping("/{code}")
    ApiResponse<PersonnelResponse> updateUser(
            @Parameter(description = "Personnel code") @PathVariable String code,
            @RequestBody PersonnelUpdateRequest request){
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.updatePersonnel(code, request))
                .build();
    }

    @Operation(summary = "Upload avatar", description = "Support file .jfif, .png, .jpeg, .jpg extension")
    @PostMapping(
            value="/upload-avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<PersonnelResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.uploadAvatar(file))
                .build();
    }

    @Operation(summary = "Delete personnel")
    @DeleteMapping("/{code}")
    public ApiResponse<String> deletePersonnel(
            @Parameter(description = "Personnel code") @PathVariable String code) {
        personnelService.deletePersonnel(code);
        return ApiResponse.<String>builder()
                .result("Personnel has been deleted")
                .build();
    }

    @PutMapping("/{code}/salary")
    public ResponseEntity<ApiResponse<PersonnelResponse>> updateBasicSalary(
            @PathVariable String code,
            @RequestBody UpdateSalaryRequest request
    ) {
        PersonnelResponse response = personnelService.updateBasicSalary(code, request);
        return ResponseEntity.ok().body(new ApiResponse<>(
                200,
                "Cập nhật lương cho nhân viên thành công",
                response
        ));
    }
}
