package com._6.ems.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;
    String sender;
    List<String> receivers;
    String sendAt;
}
