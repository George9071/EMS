package com._6.ems.controller;

import java.util.List;

import com._6.ems.dto.request.ManagerCreationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.service.ManagerService;

@Tag(name = "Managers", description = "Manager CRUD APIs")
@RestController
@RequestMapping("/managers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerController {

    ManagerService managerService;

    @Operation(summary = "Create manager")
    @PostMapping
    public ApiResponse<ManagerResponse> createManager(@RequestBody ManagerCreationRequest request) {
        return ApiResponse.<ManagerResponse>builder()
            .result(managerService.createManager(request))
            .message("Add new manager success!")
            .build();
    }

    @Operation(summary = "Get manager by code")
    @GetMapping
    public ApiResponse<ManagerResponse> getManagerByCode(
            @Parameter(description = "Manager code") @RequestParam String code) {
        return ApiResponse.<ManagerResponse>builder()
            .result(managerService.getManagerByCode(code))
            .build();
    }

    @Operation(summary = "List all managers")
    @GetMapping("/all")
    public ApiResponse<List<ManagerResponse>> getAllManager() {
        return ApiResponse.<List<ManagerResponse>>builder()
            .result(managerService.getAllManagers())
            .build();
    }
}
