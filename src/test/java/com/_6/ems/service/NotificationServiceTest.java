package com._6.ems.service;

import com._6.ems.dto.request.AdminNotificationRequest;
import com._6.ems.dto.request.NotificationRequest;
import com._6.ems.dto.response.NotificationAdminResponse;
import com._6.ems.dto.response.NotificationManagerResponse;
import com._6.ems.dto.response.SendNotificationResponse;
import com._6.ems.entity.*;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.NotificationMapper;
import com._6.ems.repository.*;
import com._6.ems.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private NotificationRecipientRepository notificationRecipientRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Manager manager;
    private Department department;
    private Employee employee;
    private Personnel managerPersonnel;
    private Personnel employeePersonnel;
    private Notification notification;
    private NotificationRequest notificationRequest;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1)
                .name("IT Department")
                .build();

        managerPersonnel = Personnel.builder()
                .code("MGR001")
                .firstName("Jane")
                .lastName("Manager")
                .email("manager@example.com")
                .build();

        manager = Manager.builder()
                .code("MGR001")
                .informationRecord(managerPersonnel)
                .department(department)
                .build();

        employeePersonnel = Personnel.builder()
                .code("EMP001")
                .firstName("John")
                .lastName("Employee")
                .email("employee@example.com")
                .build();

        employee = Employee.builder()
                .code("EMP001")
                .informationRecord(employeePersonnel)
                .department(department)
                .build();

        notification = Notification.builder()
                .id("NOTIF001")
                .sender(manager)
                .subject("Test Subject")
                .content("Test Content")
                .sendAt(OffsetDateTime.now())
                .recipients(new ArrayList<>())
                .build();

        notificationRequest = NotificationRequest.builder()
                .receivers(Arrays.asList("EMP001"))
                .subject("Test Subject")
                .message("Test Content")
                .build();
    }

    @Test
    void sendNotification_validRequest_success() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("MGR001");

            when(managerRepository.findById("MGR001")).thenReturn(Optional.of(manager));
            when(employeeRepository.findByCodeInAndDepartment_Id(anyList(), anyInt()))
                    .thenReturn(Arrays.asList(employee));
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
            when(notificationMapper.toResponse(any(Notification.class)))
                    .thenReturn(SendNotificationResponse.builder().build());
            // Remove unused email service stubbing

            // WHEN
            SendNotificationResponse response = notificationService.sendNotification(notificationRequest);

            // THEN
            assertThat(response).isNotNull();
            verify(managerRepository).findById("MGR001");
            verify(employeeRepository).findByCodeInAndDepartment_Id(anyList(), anyInt());
            verify(notificationRepository, times(2)).save(any(Notification.class));
        }
    }

    @Test
    void sendNotification_notManager_throwsException() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("EMP001");

            when(managerRepository.findById("EMP001")).thenReturn(Optional.empty());

            // WHEN
            AppException exception = assertThrows(AppException.class,
                    () -> notificationService.sendNotification(notificationRequest));

            // THEN
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_ONLY_BE_SENT_BY_MANAGER);
            verify(managerRepository).findById("EMP001");
        }
    }

    @Test
    void sendNotificationToAllExceptAdmin_validRequest_success() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::isCurrentUserAdmin).thenReturn(true);

            Account empAccount = Account.builder().id("ACC001").role(Role.EMPLOYEE).build();
            employeePersonnel.setAccount(empAccount);

            AdminNotificationRequest adminRequest = AdminNotificationRequest.builder()
                    .subject("Global Announcement")
                    .message("Important message")
                    .build();

            when(personnelRepository.findAll()).thenReturn(Arrays.asList(employeePersonnel));
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
            when(notificationMapper.toResponse(any(Notification.class)))
                    .thenReturn(SendNotificationResponse.builder().build());
            doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

            // WHEN
            SendNotificationResponse response = notificationService.sendNotificationToAllExceptAdmin(adminRequest);

            // THEN
            assertThat(response).isNotNull();
            verify(personnelRepository).findAll();
            verify(notificationRepository, times(2)).save(any(Notification.class));
        }
    }

    @Test
    void sendNotificationToAllExceptAdmin_notAdmin_throwsException() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::isCurrentUserAdmin).thenReturn(false);

            AdminNotificationRequest adminRequest = AdminNotificationRequest.builder()
                    .subject("Global Announcement")
                    .message("Important message")
                    .build();

            // WHEN
            AppException exception = assertThrows(AppException.class,
                    () -> notificationService.sendNotificationToAllExceptAdmin(adminRequest));

            // THEN
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ONLY_ADMIN_CAN_SEND_GLOBAL_NOTIFICATION);
        }
    }

    @Test
    void getAllNotificationsByManager_success() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("MGR001");

            NotificationRecipient recipient = new NotificationRecipient(notification, "employee@example.com");
            notification.setRecipients(Arrays.asList(recipient));

            when(notificationRepository.findBySender_Code("MGR001"))
                    .thenReturn(Arrays.asList(notification));

            // WHEN
            List<NotificationManagerResponse> result = notificationService.getAllNotificationsByManager();

            // THEN
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            verify(notificationRepository).findBySender_Code("MGR001");
        }
    }

    @Test
    void deleteNotificationById_asAdmin_success() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("ADMIN001");
            securityUtilMock.when(SecurityUtil::isCurrentUserAdmin).thenReturn(true);

            notification.setSender(null); // Admin notification has no sender

            when(notificationRepository.findById("NOTIF001")).thenReturn(Optional.of(notification));
            doNothing().when(notificationRepository).delete(notification);

            // WHEN
            notificationService.deleteNotificationById("NOTIF001");

            // THEN
            verify(notificationRepository).findById("NOTIF001");
            verify(notificationRepository).delete(notification);
        }
    }

    @Test
    void deleteNotificationById_notificationNotFound_throwsException() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("MGR001");

            when(notificationRepository.findById("INVALID")).thenReturn(Optional.empty());

            // WHEN
            AppException exception = assertThrows(AppException.class,
                    () -> notificationService.deleteNotificationById("INVALID"));

            // THEN
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
            verify(notificationRepository).findById("INVALID");
        }
    }

    @Test
    void deleteNotificationById_unauthorized_throwsException() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserCode).thenReturn("MGR002");
            securityUtilMock.when(SecurityUtil::isCurrentUserAdmin).thenReturn(false);

            when(notificationRepository.findById("NOTIF001")).thenReturn(Optional.of(notification));

            // WHEN
            AppException exception = assertThrows(AppException.class,
                    () -> notificationService.deleteNotificationById("NOTIF001"));

            // THEN
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(notificationRepository).findById("NOTIF001");
        }
    }

    @Test
    void getAllNotificationsAdminSent_success() {
        // GIVEN
        Notification adminNotification = Notification.builder()
                .id("NOTIF002")
                .sender(null) // Admin notifications have no sender
                .subject("Admin Notice")
                .content("Admin content")
                .build();

        when(notificationRepository.findAll()).thenReturn(Arrays.asList(adminNotification, notification));
        when(notificationMapper.toNotificationAdminResponse(any(Notification.class)))
                .thenReturn(NotificationAdminResponse.builder().build());

        // WHEN
        List<NotificationAdminResponse> result = notificationService.getAllNotificationsAdminSent();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // Only admin notification (sender == null)
        verify(notificationRepository).findAll();
    }
}
