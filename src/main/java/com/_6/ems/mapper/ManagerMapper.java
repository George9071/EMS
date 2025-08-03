package com._6.ems.mapper;

import com._6.ems.dto.request.ManagerCreationRequest;
import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.entity.Manager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManagerMapper {
    @Mapping(source = "department.id", target = "department_id")
    ManagerResponse toManagerResponse(Manager manager);
}
