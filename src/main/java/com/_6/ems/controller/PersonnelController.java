package com._6.ems.controller;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.*;
import com._6.ems.enums.Gender;
import com._6.ems.service.PersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
    public ApiResponse<PageResponse<PersonnelResponse>> getAllPersonnel(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Role of the user. Accepted values: EMPLOYEE or MANAGER"
            )
            @RequestParam(required = false)
            @Pattern(regexp = "EMPLOYEE|MANAGER", message = "EMPLOYEE hoặc MANAGER thôi nhé ae")
            String role,

            @RequestParam(required = false) Gender gender,

            @Parameter(
                    description = "Mã phòng ban"
            )
            @RequestParam(required = false) Integer department
    ) {
        var result = personnelService.getAllPersonnel(page, size, role, gender, department);
        return ApiResponse.<PageResponse<PersonnelResponse>>builder()
                .result(result)
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
