package com._6.ems.controller;

import com._6.ems.dto.request.PermissionRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.PermissionResponse;
import com._6.ems.mapper.PermissionMapper;
import com._6.ems.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PermissionController {
    PermissionService permissionService;
    PermissionMapper permissionMapper;

    @PostMapping
    ApiResponse<PermissionResponse> create(@RequestBody PermissionRequest request){
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionMapper.toPermissionResponse(permissionService.create(request)))
                .build();
    }

    @GetMapping
    ApiResponse<List<PermissionResponse>> getAll(){

        List<PermissionResponse> result = permissionService.getAll().stream()
                .map(permissionMapper::toPermissionResponse)
                .toList();

        return ApiResponse.<List<PermissionResponse>>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/{permission}")
    ApiResponse<Void> delete(@PathVariable String permission){
        permissionService.delete(permission);
        return ApiResponse.<Void>builder().build();
    }
}
