package com._6.ems.service;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.NotificationReceiverResponse;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonnelService {

    /* Repository */
    PersonnelRepository personnelRepository;
    AccountRepository accountRepository;
    PrivilegeRepository privilegeRepository;
    EmployeeRepository employeeRepository;
    PrivilegeMapper privilegeMapper;
    ManagerRepository managerRepository;
    DepartmentRepository departmentRepository;
    NotificationRecipientRepository notificationRecipientRepository;

    /* Mapper, Service, Util */
    PersonnelMapper personnelMapper;
    TaskMapper taskMapper;
    AccountService accountService;
    SalaryService salaryService;
    CloudinaryUtil cloudinaryUtil;

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

        if(request.getAccountCreationRequest().getRole() == Role.EMPLOYEE) {
            employeeRepository.save(
                    Employee.builder()
                            .informationRecord(personnel)
                    .build());
        } else if(request.getAccountCreationRequest().getRole() == Role.MANAGER) {
            managerRepository.save(
                    Manager.builder()
                            .informationRecord(personnel)
                            .build()
            );
        }

        salaryService.createMonthlySalary(personnel);

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
    }

    public PersonnelResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();

        log.info("principal: {}", context.getAuthentication().getName());
        log.info("authorities: {}", context.getAuthentication().getAuthorities());

        String accountId = context.getAuthentication().getName(); // "sub" claims
        log.info("accountId: {}", accountId);

        Account account = accountRepository.findById(accountId).orElseThrow(
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

    public List<NotificationReceiverResponse> getMyNoti() {
        String code = SecurityUtil.getCurrentUserCode();

        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        String email = personnel.getEmail();

        List<NotificationRecipient> recipients = notificationRecipientRepository.findByRecipientEmail(email);

        return recipients.stream()
                .map(recipient -> {
                    Notification notification = recipient.getNotification();
                    return NotificationReceiverResponse.builder()
                            .id(notification.getId())
                            .subject(notification.getSubject())
                            .content(notification.getContent())
                            .sendAt(notification.getSendAt())
                            .sender(notification.getSender() == null ? null : notification.getSender().getCode())
                            .isRead(recipient.isRead())
                            .build();
                })
                .toList();
    }

    @Transactional
    public NotificationReceiverResponse markAsRead(String notificationId) {
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
        return NotificationReceiverResponse.builder()
                .id(notification.getId())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .sendAt(notification.getSendAt())
                .sender(notification.getSender() == null ? null : notification.getSender().getCode())
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

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
    public PersonnelResponse updateBasicSalary(String code, UpdateSalaryRequest request) {
        Personnel personnel = personnelRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

        if(request.getBasicSalary() != null) {
            personnel.setBasicSalary(request.getBasicSalary());
        }
        if(request.getBonus() != null) {
            personnel.setBonus(request.getBonus());
        }
        if(request.getAllowance() != null) {
            personnel.setAllowance(request.getAllowance());
        }
        if(request.getKpiPenalty() != null) {
            personnel.setKpiPenalty(request.getKpiPenalty());
        }

        personnel = personnelRepository.save(personnel);

        salaryService.calculateSalary(personnel);

        return toPersonnelResponse(personnel);
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
                .basicSalary(personnel.getBasicSalary())
                .build();

        Set<PrivilegeResponse> privilegeResponses = personnel.getPrivileges() != null
                ? personnel.getPrivileges().stream()
                .map(privilegeMapper::toPrivilegeResponse)
                .collect(Collectors.toSet())
                : Collections.emptySet();
        response.setPrivileges(privilegeResponses);

        setDepartmentNameAndTasks(personnel, response);

        return response;
    }

    private void setDepartmentNameAndTasks(Personnel personnel, PersonnelResponse response) {
        Role role = (personnel.getAccount() != null) ? personnel.getAccount().getRole() : null;
        if (role == null) return;

        switch (role) {
            case EMPLOYEE -> handleEmployee(personnel, response);
            case MANAGER -> handleManager(personnel, response);
            case ADMIN -> handleAdmin(response);
            default -> { /* do nothing */ }
        }
    }

    private void handleEmployee(Personnel personnel, PersonnelResponse response) {
        employeeRepository.findById(personnel.getCode()).ifPresent(employee -> {
            response.setDepartmentName(getDepartmentName(employee.getDepartment()));
            response.setDepartmentId(employee.getDepartment() == null ? null : employee.getDepartment().getId());
            response.setTasks(employee.getTasks() != null
                    ? employee.getTasks().stream().map(taskMapper::toTaskResponse).toList()
                    : Collections.emptyList());
        });
    }

    private void handleManager(Personnel personnel, PersonnelResponse response) {
        managerRepository.findById(personnel.getCode()).ifPresent(manager -> {
            response.setDepartmentName(getDepartmentName(manager.getDepartment()));
            response.setDepartmentId(manager.getDepartment() == null ? null : manager.getDepartment().getId());
            response.setTasks(Collections.emptyList());
        });
    }

    private void handleAdmin(PersonnelResponse response) {
        response.setDepartmentName("Administration");
        response.setTasks(Collections.emptyList());
    }

    private String getDepartmentName(Department department) {
        return department != null ? department.getName() : null;
    }
}
