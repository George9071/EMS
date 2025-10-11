package com._6.ems.service;

import com._6.ems.dto.request.MeetingInvitation;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    @Async
    public void sendMeetingInvitation(MeetingInvitation meetingInvitation) {
        try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(meetingInvitation.getRecipientEmails().toArray(new String[0]));
        helper.setSubject("Thông báo cuộc họp: " + meetingInvitation.getMeetingTitle());
        helper.setFrom("bk.manarate@gmail.com");

        Context context = new Context();
        context.setVariable("meetingTitle", meetingInvitation.getMeetingTitle());
        context.setVariable("meetingDescription", meetingInvitation.getMeetingDescription());
        context.setVariable("organizer", meetingInvitation.getOrganizer());
        context.setVariable("organizerDepartment", meetingInvitation.getOrganizerDepartment());
        context.setVariable("startTime", meetingInvitation.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        context.setVariable("endTime", meetingInvitation.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        context.setVariable("roomName", meetingInvitation.getRoomName());
        context.setVariable("roomLocation", meetingInvitation.getRoomLocation());
        context.setVariable("duration", calculateDuration(meetingInvitation.getStartTime(), meetingInvitation.getEndTime()));
        context.setVariable("capacity", meetingInvitation.getCapacity());

        String htmlContent = templateEngine.process("meeting-invitation", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        } catch (MessagingException e) {
            throw new AppException(ErrorCode.EMAIL_EXCEPTION);
        }
    }

    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            throw new AppException(ErrorCode.EMAIL_EXCEPTION);
        }
    }

    private String calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return hours + " giờ " + remainingMinutes + " phút";
        } else {
            return remainingMinutes + " phút";
        }
    }
}
