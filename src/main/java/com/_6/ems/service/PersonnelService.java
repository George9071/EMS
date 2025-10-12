package com._6.ems.service;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.response.NotiResponse;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.dto.response.PrivilegeResponse;
import com._6.ems.entity.*;
import com._6.ems.entity.compositeKey.NotificationRecipientId;
import com._6.ems.enums.PrivilegeName;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.PersonnelMapper;
import com._6.ems.mapper.PrivilegeMapper;
import com._6.ems.mapper.TaskMapper;
import com._6.ems.repository.*;
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
import java.util.*;
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
    EmployeeRepository employeeRepository;
    PrivilegeMapper privilegeMapper;
    ManagerRepository managerRepository;
    private final TaskMapper taskMapper;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public PersonnelResponse createPersonnel(PersonnelCreationRequest request) {
        Account account = accountService.createAccount(request.getAccountCreationRequest());
        Personnel personnel = personnelMapper.toPersonnel(request);

        personnel.setCode(generateCode(request.getFirstName(), request.getLastName(), request.getDob()));
        personnel.setAccount(account);

        Privilege privilege = privilegeRepository.findByName(PrivilegeName.valueOf("EMPLOYEE"))
                .orElseThrow(() -> new AppException(ErrorCode.PRIVILEGE_NOT_FOUND));
        personnel.setPosition(request.getPosition());
        personnel.setPrivileges(Set.of(privilege));

        personnel = personnelRepository.save(personnel);

        return toPersonnelResponse(personnel);
    }

    @Transactional
    public void deletePersonnel(String code) {
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

        employeeRepository.findByCode(code).ifPresent(employee -> {
            Department department  = employee.getDepartment();
            if (department != null) {
                if (department.getEmployees() != null) department.removeEmployee(employee);
                employee.setDepartment(null);
                departmentRepository.save(department);
            }

            employeeRepository.delete(employee);
        });

        Account account = personnel.getAccount();
        if (account != null) accountRepository.delete(account);
        personnelRepository.delete(personnel);

        return toPersonnelResponse(personnel);
    }

    public PersonnelResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        Account account = accountRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        Personnel personnel = personnelRepository.findByAccount_Id(account.getId()).orElseThrow(
                () -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        return toPersonnelResponse(personnel);
    }

    @Transactional
    public PersonnelResponse updatePersonnel(String code, PersonnelUpdateRequest request){
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        personnelMapper.updatePersonnel(personnel, request);

        if (request.getPrivileges() != null) {
            var privileges = privilegeRepository.findAllById(request.getPrivileges());
            personnel.setPrivileges(new HashSet<>(privileges));
        }

        return toPersonnelResponse(personnel);
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

            return toPersonnelResponse(personnel);
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
                .map(this::toPersonnelResponse)
                .toList();
    }

    public PersonnelResponse getPersonnelByCode(String code) {
        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        return toPersonnelResponse(personnel);
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

    public PersonnelResponse toPersonnelResponse(Personnel personnel) {
        if (personnel == null) {
            return null;
        }

        PersonnelResponse response = PersonnelResponse.builder()
                .code(personnel.getCode())
                .firstName(personnel.getFirstName())
                .lastName(personnel.getLastName())
                .gender(personnel.getGender() != null ? personnel.getGender().name() : null)
                .avatarUrl(personnel.getAvatar())
                .dob(personnel.getDob())
                .email(personnel.getEmail())
                .phoneNumber(personnel.getPhoneNumber())
                .city(personnel.getCity())
                .street(personnel.getStreet())
                .description(personnel.getDescription())
                .skills(personnel.getSkills())
                .position(personnel.getPosition())
                .accountId(personnel.getAccount() != null ? personnel.getAccount().getId() : null)
                .role(personnel.getAccount() != null ? personnel.getAccount().getRole().name() : null)
                .build();

        Set<PrivilegeResponse> privilegeResponses = personnel.getPrivileges() != null
                ? personnel.getPrivileges().stream()
                .map(privilegeMapper::toPrivilegeResponse)
                .collect(Collectors.toSet())
                : Collections.emptySet();
        response.setPrivileges(privilegeResponses);

        Role role = personnel.getAccount() != null ? personnel.getAccount().getRole() : null;

        if (role == Role.EMPLOYEE) {
            Employee employee = employeeRepository.findById(personnel.getCode()).orElse(null);
            if (employee != null) {
                response.setDepartmentName(
                        employee.getDepartment() != null ? employee.getDepartment().getName() : null
                );
                response.setTasks(employee.getTasks() != null ? employee.getTasks().stream().map(taskMapper::toTaskResponse).toList() : Collections.emptyList());
            }

        } else if (role == Role.MANAGER) {
            Manager manager = managerRepository.findById(personnel.getCode()).orElse(null);
            if (manager != null) {
                response.setDepartmentName(
                        manager.getDepartment() != null ? manager.getDepartment().getName() : null
                );
                response.setTasks(Collections.emptyList());
            }

        } else if (role == Role.ADMIN) {
            response.setDepartmentName("Administration");
            response.setTasks(Collections.emptyList());
        }

        return response;
    }
}
