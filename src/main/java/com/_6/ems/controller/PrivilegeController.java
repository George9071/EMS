package com._6.ems.controller;

import com._6.ems.dto.request.PrivilegeRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.dto.response.PermissionResponse;
import com._6.ems.dto.response.PrivilegeResponse;
import com._6.ems.mapper.PrivilegeMapper;
import com._6.ems.service.PrivilegeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privileges")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PrivilegeController {
    PrivilegeService privilegeService;
    PrivilegeMapper privilegeMapper;

    @PostMapping
    ApiResponse<PrivilegeResponse> create(@RequestBody PrivilegeRequest request){
        return ApiResponse.<PrivilegeResponse>builder()
                .result(privilegeMapper.toPrivilegeResponse(privilegeService.create(request)))
                .build();
    }

    @GetMapping
    ApiResponse<List<PrivilegeResponse>> getAll(){
        List<PrivilegeResponse> result = privilegeService.getAll().stream()
                .map(privilegeMapper::toPrivilegeResponse)
                .toList();

        return ApiResponse.<List<PrivilegeResponse>>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/{privilege}")
    ApiResponse<Void> delete(@PathVariable String privilege){
        privilegeService.delete(privilege);
        return ApiResponse.<Void>builder().build();
    }
}
