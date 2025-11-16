package com._6.ems.dto.request;

import com._6.ems.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectUpdateRequest {

    @Schema(description = "Tên của dự án")
    String name;

    @Schema(description = "Mô tả về dự án.")
    String description;

    @Schema(description = "Số lượng thành viên tối đa có thể tham gia dự án")
    Integer maxParticipants;

    @Schema(description = "Ngày bắt đầu dự án (format: yyyy-MM-dd)")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc dự án (format: yyyy-MM-dd)")
    LocalDate endDate;

    @Schema(description = "Tiến độ dự án. (PLANNED, DEVELOPED, IN_PROGRESS, ON_HOLD, CLOSED)")
    ProjectStatus status;
}
