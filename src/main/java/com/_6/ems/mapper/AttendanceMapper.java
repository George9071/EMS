package com._6.ems.mapper;

import com._6.ems.dto.response.AttendanceRecordDTO;
import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.entity.AttendanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    double STANDARD_WORK_HOURS = 8.0;


    @Mapping(target = "record_id", source = "id")
    @Mapping(target = "employee_code", source = "personnel.code")
    @Mapping(target = "checkIn", source = "checkIn", qualifiedByName = "formatDateTime")
    @Mapping(target = "checkOut", source = "checkOut", qualifiedByName = "formatDateTime")
    @Mapping(target = "duration",
            expression = "java(formatDuration(record.getCheckIn(), record.getCheckOut()))")
    AttendanceRecordResponse toAttendanceRecordResponse(AttendanceRecord record);

    @Mapping(target = "date", source = "date")
    @Mapping(target = "day", expression = "java(getDayOfWeekInEnglish(record.getDate()))")
    @Mapping(target = "checkIn", expression = "java(toLocalTime(record.getCheckIn()))")
    @Mapping(target = "checkOut", expression = "java(toLocalTime(record.getCheckOut()))")
    @Mapping(target = "workHours", expression = "java(formatWorkHours(record.getWorkHours()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "notEnoughHour", expression = "java(isNotEnoughHour(record.getWorkHours()))")
    AttendanceRecordDTO toDTO(AttendanceRecord record);

    default List<AttendanceRecordDTO> toDTOList(List<AttendanceRecord> records) {
        return records == null ? List.of() :
                records.stream()
                        .map(this::toDTO)
                        .collect(Collectors.toList());
    }


    // --- Helper methods ---
    @Named("formatDateTime")
    default String formatDateTime(OffsetDateTime time) {
        return time == null ? null : time.format(DT);
    }

    default String formatDuration(OffsetDateTime in, OffsetDateTime out) {
        if (in == null || out == null) return "00:00:00";
        if (out.isBefore(in)) return "00:00:00";
        Duration d = Duration.between(in, out);
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }

    @Named("toLocalTime")
    default LocalTime toLocalTime(java.time.OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    @Named("getDayOfWeekInEnglish")
    default String getDayOfWeekInEnglish(LocalDate date) {
        if (date == null) return "";

        return date.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
        );
    }

    @Named("formatWorkHours")
    default String formatWorkHours(Double hours) {
        if (hours == null || hours == 0) return "0m";

        int hourPart = hours.intValue();
        int minutePart = (int) ((hours - hourPart) * 60);

        if (minutePart == 0) return hourPart + "h";

        return hourPart + "h " + minutePart + "m";
    }

    @Named("isNotEnoughHour")
    default boolean isNotEnoughHour(Double workHours) {
        return workHours != null && workHours < STANDARD_WORK_HOURS;
    }
}
