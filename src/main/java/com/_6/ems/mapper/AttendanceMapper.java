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

    @Mapping(target = "record_id", source = "id")
    @Mapping(target = "employee_code", source = "personnel.code")
    @Mapping(target = "checkIn", source = "checkIn", qualifiedByName = "formatDateTime")
    @Mapping(target = "checkOut", source = "checkOut", qualifiedByName = "formatDateTime")
    @Mapping(target = "duration", expression = "java(formatDuration(attendanceRecord.getCheckIn(), attendanceRecord.getCheckOut()))")
    AttendanceRecordResponse toAttendanceRecordResponse(AttendanceRecord attendanceRecord);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    default String formatDuration(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) return "00:00:00";
        Duration duration = Duration.between(checkIn, checkOut);
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
