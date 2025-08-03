package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String subject;

    @Column(nullable = false)
    String content;

    @Column(name = "send_at", nullable = false, updatable = false)
    @Builder.Default
    LocalDateTime sendAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_code", referencedColumnName = "code")
    Manager sender;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<NotificationRecipient> recipients = new ArrayList<>();
}
