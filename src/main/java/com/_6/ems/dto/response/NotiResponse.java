package com._6.ems.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotiResponse {
    String id;
    String subject;
    String content;
    LocalDateTime sendAt;
    String sender;
    boolean isRead;
}
