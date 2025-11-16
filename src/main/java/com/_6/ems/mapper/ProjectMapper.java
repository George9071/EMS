package com._6.ems.mapper;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.request.ProjectUpdateRequest;
import com._6.ems.dto.response.ProjectResponse;
import com._6.ems.entity.Project;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "status", target = "status")
    ProjectResponse toProjectResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(source = "maxParticipants", target = "maxParticipants")
    @Mapping(target = "participants", constant = "0")
    @Mapping(source = "startDate", target = "startDate")
    @Mapping(source = "endDate", target = "endDate")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Project toProject(ProjectCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProject(ProjectUpdateRequest request, @MappingTarget Project project);
}
