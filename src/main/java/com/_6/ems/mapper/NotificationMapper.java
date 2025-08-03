package com._6.ems.mapper;

import com._6.ems.dto.response.NotificationResponse;
import com._6.ems.entity.Notification;
import com._6.ems.entity.NotificationRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "sender.code", target = "sender")
    @Mapping(source = "recipients", target = "receivers", qualifiedByName = "mapRecipientsToEmails")
    @Mapping(source = "sendAt", target = "sendAt", qualifiedByName = "formatDateTime")
    NotificationResponse toResponse(Notification notification);

    @Named("formatDateTime")
    static String formatDateTime(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    @Named("mapRecipientsToEmails")
    default List<String> mapRecipientsToEmails(List<NotificationRecipient> recipients) {
        if (recipients == null) return new ArrayList<>();
        return recipients.stream()
                .map(r -> r.getId().getRecipientEmail())
                .collect(Collectors.toList());
    }
}
