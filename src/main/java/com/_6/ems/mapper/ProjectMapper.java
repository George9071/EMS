package com._6.ems.mapper;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.ProjectResponse;
import com._6.ems.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    @Mapping(source = "department.name", target = "department_name")
    @Mapping(source = "department.id", target = "department_id")
    @Mapping(source = "status", target = "status")
    ProjectResponse toProjectResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(source = "max_participants", target = "maxParticipants")
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "startDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Project toProject(ProjectCreationRequest request);
}
