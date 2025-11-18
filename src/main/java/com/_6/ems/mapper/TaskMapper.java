package com._6.ems.mapper;

import com._6.ems.dto.request.TaskCreationRequest;
import com._6.ems.dto.request.TaskUpdateRequest;
import com._6.ems.dto.response.TaskResponse;
import com._6.ems.entity.Task;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "assignee.code", target = "assigneeCode")
    TaskResponse toTaskResponse(Task task);

    @Mapping(source = "projectId", target = "project.id")
    @Mapping(target = "assignee.code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "lastStatusChange", ignore = true)
    @Mapping(target = "files", ignore = true)
    Task toTask(TaskCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTask(TaskUpdateRequest request, @MappingTarget Task task);
}
