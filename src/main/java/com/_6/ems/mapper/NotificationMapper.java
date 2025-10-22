package com._6.ems.mapper;

import com._6.ems.dto.response.NotificationAdminResponse;
import com._6.ems.dto.response.SendNotificationResponse;
import com._6.ems.entity.Notification;
import com._6.ems.entity.NotificationRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "sender.code", target = "sender")
    @Mapping(source = "recipients", target = "receivers", qualifiedByName = "mapRecipientsToEmails")
    @Mapping(source = "sendAt", target = "sendAt", qualifiedByName = "formatDateTime")
    SendNotificationResponse toResponse(Notification notification);

    @Named("formatDateTime")
    static String formatDateTime(OffsetDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    @Named("mapRecipientsToEmails")
    default List<String> mapRecipientsToEmails(List<NotificationRecipient> recipients) {
        if (recipients == null) return new ArrayList<>();
        return recipients.stream()
                .map(r -> r.getId().getRecipientEmail())
                .toList();
    }

    NotificationAdminResponse toNotificationAdminResponse(Notification notification);
}
