package com._6.ems.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationReceiverResponse {
    String id;
    String subject;
    String content;
    OffsetDateTime sendAt;
    String sender;
    boolean isRead;
}
