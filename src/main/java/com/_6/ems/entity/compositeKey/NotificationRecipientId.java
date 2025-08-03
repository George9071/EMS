package com._6.ems.entity.compositeKey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NotificationRecipientId implements Serializable {
    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "recipient_email")
    private String recipientEmail;
}
