package com._6.ems.service;

import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.NotificationResponse;
import com._6.ems.entity.*;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.NotificationMapper;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ManagerRepository;
import com._6.ems.repository.NotificationRepository;
import com._6.ems.utils.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    JavaMailSender mailSender;
    NotificationMapper notificationMapper;

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
                .sendAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        Notification finalNotification = notification;

        List<NotificationRecipient> recipientEntities = emails.stream()
                .map(email -> new NotificationRecipient(finalNotification, email))
                .collect(Collectors.toCollection(ArrayList::new));

        notification.setRecipients(recipientEntities);

        notificationRepository.save(notification);

        for (String email : emails) {
            sendEmail(email, request.getSubject(), request.getMessage());
        }

        return notificationMapper.toResponse(notification);
    }

    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
