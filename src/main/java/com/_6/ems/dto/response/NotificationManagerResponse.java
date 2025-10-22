package com._6.ems.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationManagerResponse {
    String id;
    String subject;
    String content;
    OffsetDateTime sendAt;
    List<String> recipients;
}
