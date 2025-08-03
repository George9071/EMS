package com._6.ems.mapper;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(source = "project.id", target = "project_id")
    @Mapping(source = "assignee.code", target = "assignee_code")
    TaskResponse toTaskResponse(Task task);

    @Mapping(source = "project_id", target = "project.id")
    @Mapping(target = "assignee.code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "lastStatusChange", ignore = true)
    @Mapping(target = "files", ignore = true)
    Task toTask(TaskCreationRequest request);
}
