package com._6.ems.mapper;

import com._6.ems.dto.response.FileResponse;
import com._6.ems.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {

    @Mappings({
            @Mapping(source = "id", target = "fileId"),
            @Mapping(source = "task.id", target = "taskId"),
            @Mapping(source = "uploader.code", target = "uploaderCode"),
            @Mapping(source = "url", target = "fileUrl"),
            @Mapping(source = "name", target = "fileName"),
            @Mapping(source = "type", target = "fileType")
    })
    FileResponse toResponse(File file);
}
