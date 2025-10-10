package com._6.ems.entity;

import com._6.ems.entity.compositeKey.NotificationRecipientId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notification_recipient")
public class NotificationRecipient {
    @EmbeddedId
    NotificationRecipientId id;

    @Column(name = "is_read")
    @Builder.Default
    boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("notificationId")
    @JoinColumn(name = "notification_id")
    Notification notification;

    public NotificationRecipient(Notification notification, String email) {
        this.notification = notification;
        this.id = new NotificationRecipientId(notification.getId(), email);
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = new NotificationRecipientId();
        }
        if (this.notification != null && this.id.getNotificationId() == null) {
            this.id.setNotificationId(this.notification.getId());
        }
    }
}
