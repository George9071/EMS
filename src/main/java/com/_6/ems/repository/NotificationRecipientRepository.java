package com._6.ems.repository;

import com._6.ems.entity.Notification;
import com._6.ems.entity.NotificationRecipient;
import com._6.ems.entity.compositeKey.NotificationRecipientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRecipientRepository extends
        JpaRepository<NotificationRecipient, NotificationRecipientId> {

    @Query("SELECT nr.notification FROM NotificationRecipient nr WHERE nr.id.recipientEmail = :email")
    List<Notification> findNotificationsByRecipientEmail(@Param("email") String email);

    @Query("SELECT nr FROM NotificationRecipient nr WHERE nr.id.recipientEmail = :email")
    List<NotificationRecipient> findByRecipientEmail(@Param("email") String email);
}
