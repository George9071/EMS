package com._6.ems.mapper;

import com._6.ems.dto.request.PermissionRequest;
import com._6.ems.dto.response.PermissionResponse;
import com._6.ems.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
