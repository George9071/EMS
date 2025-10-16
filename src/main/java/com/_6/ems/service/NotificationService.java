package com._6.ems.service;

import com._6.ems.dto.request.AdminNotificationRequest;
import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.NotificationResponse;
import com._6.ems.entity.*;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.NotificationMapper;
import com._6.ems.repository.*;
import com._6.ems.utils.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    ManagerRepository managerRepository;
    EmployeeRepository employeeRepository;
    NotificationRepository notificationRepository;
    EmailService emailService;
    NotificationMapper notificationMapper;
    PersonnelRepository personnelRepository;
    NotificationRecipientRepository notificationRecipientRepository;

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        String code = SecurityUtil.getCurrentUserCode();

        Manager manager = managerRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_ONLY_BE_SENT_BY_MANAGER));

        List<Employee> receivers = employeeRepository.findByCodeInAndDepartment_Id(
                request.getReceivers(),
                manager.getDepartment().getId()
        );

        List<String> emails = receivers.stream()
                .map(record -> record.getInformationRecord().getEmail())
                .filter(Objects::nonNull)
                .toList();

        Notification notification = Notification.builder()
                .sender(manager)
                .subject(request.getSubject())
                .content(request.getMessage())
                .sendAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        notification = notificationRepository.save(notification);

        Notification finalNotification = notification;

        List<NotificationRecipient> recipientEntities = emails.stream()
                .map(email -> new NotificationRecipient(finalNotification, email))
                .collect(Collectors.toCollection(ArrayList::new));

        notification.setRecipients(recipientEntities);

        notificationRepository.save(notification);

        for (String email : emails) {
            emailService.sendEmail(email, request.getSubject(), request.getMessage());
        }

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationResponse sendNotificationToAllExceptAdmin(AdminNotificationRequest request) {

        if (!SecurityUtil.isCurrentUserAdmin()) {
            throw new AppException(ErrorCode.ONLY_ADMIN_CAN_SEND_GLOBAL_NOTIFICATION);
        }

        List<Personnel> allPersonnels = personnelRepository.findAll()
                .stream()
                .filter(emp -> emp.getAccount() != null && emp.getAccount().getRole() != Role.ADMIN)
                .toList();

        if (allPersonnels.isEmpty()) {
            throw new AppException(ErrorCode.NO_RECIPIENT_FOUND);
        }

        List<String> emails = allPersonnels.stream()
                .map(Personnel::getEmail)
                .filter(Objects::nonNull)
                .toList();

        // 👉 Lưu Notification 1 lần
        Notification notification = Notification.builder()
                .sender(null)
                .subject(request.getSubject())
                .content(request.getMessage())
                .sendAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        notification = notificationRepository.save(notification);

        Notification finalNotification = notification;

        List<NotificationRecipient> recipientEntities = emails.stream()
                .map(email -> new NotificationRecipient(finalNotification, email))
                .collect(Collectors.toCollection(ArrayList::new));

        notification.setRecipients(recipientEntities);

        notificationRepository.save(notification);

        for (String email : emails) {
            emailService.sendEmail(email, request.getSubject(), request.getMessage());
        }

        return notificationMapper.toResponse(notification);
    }
}
