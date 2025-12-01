package com._6.ems.mapper;

import com._6.ems.dto.response.AiReviewResponse;
import com._6.ems.dto.response.FileResponse;
import com._6.ems.dto.response.UploadFileResponse;
import com._6.ems.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {

    @Mapping(source = "id", target = "fileId")
    @Mapping(source = "task.id", target = "taskId")
    @Mapping(source = "uploader.code", target = "uploaderCode")
    @Mapping(source = "uploadTime", target = "uploadTime")
    @Mapping(source = "url", target = "fileUrl")
    @Mapping(source = "name", target = "fileName")
    @Mapping(source = "type", target = "fileType")
    @Mapping(target = "aiReviewResponse", expression = "java(mapAiReview(file))")
    UploadFileResponse toUploadFileResponse(File file);

    default AiReviewResponse mapAiReview(File file) {
        if (file.getAiReview() == null && file.getAiScore() == null) {
            return null;
        }

        return AiReviewResponse.builder()
                .review(file.getAiReview())
                .score(file.getAiScore())
                .strengths(new ArrayList<>())
                .improvements(new ArrayList<>())
                .build();
    }

    @Mapping(source = "id", target = "fileId")
    @Mapping(source = "task.id", target = "taskId")
    @Mapping(source = "uploader.code", target = "uploaderCode")
    @Mapping(source = "uploadTime", target = "uploadTime")
    @Mapping(source = "url", target = "fileUrl")
    @Mapping(source = "name", target = "fileName")
    @Mapping(source = "type", target = "fileType")
    @Mapping(source = "aiReview", target = "aiReview")
    @Mapping(source = "aiScore", target = "aiScore")
    FileResponse toFileResponse(File file);
}
