package com._6.ems.controller;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.*;
import com._6.ems.mapper.PersonnelMapper;
import com._6.ems.service.PersonnelService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/personnels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonnelController {
    PersonnelService personnelService;

    @GetMapping("/myInfo")
    ApiResponse<PersonnelResponse> getMyInfo(){
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.getMyInfo())
                .build();
    }

    @GetMapping("/notifications")
    ApiResponse<List<NotiResponse>> getMyNotification(){
        return ApiResponse.<List<NotiResponse>>builder()
                .result(personnelService.getMyNoti())
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<PersonnelResponse>> getAllPersonnel() {
        List<PersonnelResponse> personnel = personnelService.getAllPersonnel();
        return ApiResponse.<List<PersonnelResponse>>builder()
                .result(personnel)
                .build();
    }

    @GetMapping("/{code}")
    public ApiResponse<PersonnelResponse> getPersonnelByCode(@PathVariable String code) {
        PersonnelResponse personnel = personnelService.getPersonnelByCode(code);
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnel)
                .build();
    }

    @PostMapping("/notifications/{notificationId}")
    public ApiResponse<NotiResponse> markAsRead(@PathVariable String notificationId) {
        return ApiResponse.<NotiResponse>builder()
                .result(personnelService.markAsRead(notificationId))
                .build();
    }

    @PostMapping
    ApiResponse<PersonnelResponse> createPersonnel(@RequestBody @Valid PersonnelCreationRequest request){
        ApiResponse<PersonnelResponse> apiResponse = new ApiResponse<>();

        apiResponse.setResult(personnelService.createPersonnel(request));

        return apiResponse;
    }

    @PatchMapping("/{code}")
    ApiResponse<PersonnelResponse> updateUser(@PathVariable String code, @RequestBody PersonnelUpdateRequest request){
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.updatePersonnel(code, request))
                .build();
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<PersonnelResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<PersonnelResponse>builder()
                .result(personnelService.uploadAvatar(file))
                .build();
    }

    @DeleteMapping("/{code}")
    public ApiResponse<String> deletePersonnel(@PathVariable String code) {
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
