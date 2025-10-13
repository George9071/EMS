package com._6.ems.mapper;

import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.entity.AttendanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "record_id", source = "id")
    @Mapping(target = "employee_code", source = "personnel.code")
    @Mapping(target = "checkIn", source = "checkIn", qualifiedByName = "formatDateTime")
    @Mapping(target = "checkOut", source = "checkOut", qualifiedByName = "formatDateTime")
    @Mapping(target = "duration",
            expression = "java(formatDuration(record.getCheckIn(), record.getCheckOut()))")
    AttendanceRecordResponse toAttendanceRecordResponse(AttendanceRecord record);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DT);
    }

    default String formatDuration(LocalDateTime in, LocalDateTime out) {
        if (in == null || out == null) return "00:00:00";
        if (out.isBefore(in)) return "00:00:00";
        Duration d = Duration.between(in, out);
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }
}
