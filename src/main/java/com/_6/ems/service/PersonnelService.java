package com._6.ems.service;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.response.NotiResponse;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.entity.*;
import com._6.ems.entity.compositeKey.NotificationRecipientId;
import com._6.ems.enums.PrivilegeName;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.PersonnelMapper;
import com._6.ems.repository.AccountRepository;
import com._6.ems.repository.NotificationRecipientRepository;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.PrivilegeRepository;
import com._6.ems.utils.CloudinaryUtil;
import com._6.ems.utils.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonnelService {

    PersonnelRepository personnelRepository;
    AccountRepository accountRepository;
    PrivilegeRepository privilegeRepository;
    PersonnelMapper personnelMapper;
    AccountService accountService;
    CloudinaryUtil cloudinaryUtil;
    NotificationRecipientRepository notificationRecipientRepository;

    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
    public Personnel createPersonnel(PersonnelCreationRequest request) {
        Account account = accountService.createAccount(request.getAccountCreationRequest());
        Personnel personnel = personnelMapper.toPersonnel(request);

        personnel.setCode(generateCode(request.getFirstName(), request.getLastName(), request.getDob()));
        personnel.setAccount(account);

        Privilege privilege = privilegeRepository.findByName(PrivilegeName.valueOf("EMPLOYEE"))
                .orElseThrow(() -> new AppException(ErrorCode.PRIVILEGE_NOT_FOUND));
        personnel.setPosition(request.getPosition());
        personnel.setPrivileges(Set.of(privilege));

        return personnelRepository.save(personnel);
    }

    @Transactional
    public PersonnelResponse deletePersonnel(String code) {
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
        Account account = personnel.getAccount();
        accountRepository.delete(account);
        personnelRepository.delete(personnel);
        return personnelMapper.toPersonnelResponse(personnel);
    }

    public Personnel getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        Account account = accountRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        return personnelRepository.findByAccount_Id(account.getId()).orElseThrow(
                () -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));
    }

    @Transactional
    public Personnel updatePersonnel(String code, PersonnelUpdateRequest request){
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        personnelMapper.updatePersonnel(personnel, request);

        if (request.getPrivileges() != null) {
            var privileges = privilegeRepository.findAllById(request.getPrivileges());
            personnel.setPrivileges(new HashSet<>(privileges));
        }

        return personnelRepository.save(personnel);
    }

    @Transactional
    public PersonnelResponse uploadAvatar(MultipartFile file) {
        String code = SecurityUtil.getCurrentUserCode();

        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        cloudinaryUtil.validateFile(file);

        String format = cloudinaryUtil.getUploadFormat(file.getContentType(), file.getOriginalFilename());

        try {
            String imageUrl = cloudinaryUtil.uploadToCloudinary(file, format);

            personnel.setAvatar(imageUrl);
            personnelRepository.save(personnel);

            return personnelMapper.toPersonnelResponse(personnel);
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    public List<NotiResponse> getMyNoti() {
        String code = SecurityUtil.getCurrentUserCode();

        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        String email = personnel.getEmail();

        List<NotificationRecipient> recipients = notificationRecipientRepository.findByRecipientEmail(email);

        return recipients.stream()
                .map(recipient -> {
                    Notification notification = recipient.getNotification();
                    return NotiResponse.builder()
                            .id(notification.getId())
                            .subject(notification.getSubject())
                            .content(notification.getContent())
                            .sendAt(notification.getSendAt())
                            .sender(notification.getSender().getCode())
                            .isRead(recipient.isRead())
                            .build();
                })
                .toList();
    }

    @Transactional
    public NotiResponse markAsRead(String notificationId) {
        String userCode = SecurityUtil.getCurrentUserCode();

        Personnel personnel = personnelRepository.findById(userCode)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        String email = personnel.getEmail();

        NotificationRecipientId recipientId = new NotificationRecipientId(notificationId, email);

        NotificationRecipient recipient = notificationRecipientRepository.findById(recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!recipient.isRead()) {
            recipient.setRead(true);
            notificationRecipientRepository.save(recipient);
        }

        Notification notification = recipient.getNotification();
        return NotiResponse.builder()
                .id(notification.getId())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .sendAt(notification.getSendAt())
                .sender(notification.getSender().getCode())
                .isRead(recipient.isRead())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
    public List<PersonnelResponse> getAllPersonnel() {
        return personnelRepository.findAll()
                .stream()
                .map(personnelMapper::toPersonnelResponse)
                .toList();
    }

    public PersonnelResponse getPersonnelByCode(String code) {
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));
        return personnelMapper.toPersonnelResponse(personnel);
    }

    /* Helper method */
    private String generateCode(String firstName, String lastName, LocalDate dob) {
        String first = firstName != null && !firstName.isEmpty() ? firstName.substring(0, 1).toUpperCase() : "X";
        String second = lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1).toUpperCase() : "X";

        String dayMonth = String.format("%02d%02d", dob.getDayOfMonth(), dob.getMonthValue());

        int randomNumber = new Random().nextInt(10000); // 0 to 9999
        String randomDigits = String.format("%04d", randomNumber);

        return first + second + dayMonth + randomDigits;
    }
}
