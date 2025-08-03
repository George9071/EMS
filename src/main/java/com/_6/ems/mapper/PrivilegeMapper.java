package com._6.ems.mapper;

import com._6.ems.dto.request.PrivilegeRequest;
import com._6.ems.dto.response.PrivilegeResponse;
import com._6.ems.entity.Privilege;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PrivilegeMapper {
    @Mapping(target = "permissions", ignore = true)
    Privilege toPrivilege(PrivilegeRequest request);

    PrivilegeResponse toPrivilegeResponse(Privilege privilege);
}
