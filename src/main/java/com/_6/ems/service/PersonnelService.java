package com._6.ems.service;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.NotificationReceiverResponse;
import com._6.ems.dto.response.PageResponse;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.entity.*;
import com._6.ems.entity.compositeKey.NotificationRecipientId;
import com._6.ems.enums.Gender;
import com._6.ems.enums.PrivilegeName;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.PersonnelMapper;
import com._6.ems.repository.*;
import com._6.ems.utils.CloudinaryUtil;
import com._6.ems.utils.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

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
    ManagerRepository managerRepository;
    DepartmentRepository departmentRepository;
    NotificationRecipientRepository notificationRecipientRepository;

    /* Mapper, Service, Util */
    PersonnelMapper personnelMapper;
    AccountService accountService;
    SalaryService salaryService;
    CloudinaryUtil cloudinaryUtil;
    MeetingBookingRepository meetingBookingRepository;

    @Transactional
    public PersonnelResponse createPersonnel(PersonnelCreationRequest request) {
        if(personnelRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Account account = accountService.createAccount(request.getAccountCreationRequest());

        Personnel personnel = personnelMapper.toPersonnel(request);

        personnel.setCode(generateCode(request.getFirstName(), request.getLastName(), request.getDob()));
        personnel.setAccount(account);

        Role accountRole = request.getAccountCreationRequest().getRole();
        Privilege privilege = privilegeRepository.findByName(PrivilegeName.valueOf(accountRole.toString()))
                .orElseThrow(() -> new AppException(ErrorCode.PRIVILEGE_NOT_FOUND));

        personnel.setPosition(request.getPosition());
        personnel.setPrivileges(Set.of(privilege));

        personnel = personnelRepository.save(personnel);

        if (accountRole == Role.EMPLOYEE) {
            employeeRepository.save(Employee.builder().informationRecord(personnel).build());
        } else if(accountRole == Role.MANAGER) {
            managerRepository.save(Manager.builder().informationRecord(personnel).build());
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
            meetingBookingRepository.deleteByOrganizerCode(code);
            employeeRepository.delete(employee);
        });

        Account account = personnel.getAccount();
        if (account != null) accountRepository.delete(account);
        personnelRepository.delete(personnel);
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER')")
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

        try {
            String imageUrl = cloudinaryUtil.uploadToCloudinary(file, "avatar");

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
    public PageResponse<PersonnelResponse> getAllPersonnel(
            int page, int size, String role, Gender gender, String departmentName, String keyword) {

        if (page < 1) page = 1;
        if (size < 1) size = 10;

        Sort sort = Sort.by("code").ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Specification<Personnel> spec = Specification.where(null);

        if (role != null && !role.isEmpty()) {
            if (role.equalsIgnoreCase("EMPLOYEE")) {
                spec = spec.and((root, query, cb) -> {
                    var subquery = query.subquery(String.class);
                    var employeeRoot = subquery.from(com._6.ems.entity.Employee.class);
                    subquery.select(employeeRoot.get("code"));

                    if (departmentName != null && !departmentName.isEmpty()) {
                        subquery.where(cb.equal(
                                cb.lower(employeeRoot.get("department").get("name")),
                                departmentName.toLowerCase()
                        ));
                    }

                    return cb.in(root.get("code")).value(subquery);
                });
            } else if (role.equalsIgnoreCase("MANAGER")) {
                spec = spec.and((root, query, cb) -> {
                    var subquery = query.subquery(String.class);
                    var managerRoot = subquery.from(com._6.ems.entity.Manager.class);
                    subquery.select(managerRoot.get("code"));

                    if (departmentName != null && !departmentName.isEmpty()) {
                        subquery.where(cb.equal(
                                cb.lower(managerRoot.get("department").get("name")),
                                departmentName.toLowerCase()
                        ));
                    }

                    return cb.in(root.get("code")).value(subquery);
                });
            }
        } else if (departmentName != null && !departmentName.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                var employeeSubquery = query.subquery(String.class);
                var employeeRoot = employeeSubquery.from(com._6.ems.entity.Employee.class);
                employeeSubquery.select(employeeRoot.get("code"))
                        .where(cb.equal(
                                cb.lower(employeeRoot.get("department").get("name")),
                                departmentName.toLowerCase()
                        ));

                var managerSubquery = query.subquery(String.class);
                var managerRoot = managerSubquery.from(com._6.ems.entity.Manager.class);
                managerSubquery.select(managerRoot.get("code"))
                        .where(cb.equal(
                                cb.lower(managerRoot.get("department").get("name")),
                                departmentName.toLowerCase()
                        ));

                return cb.or(
                        cb.in(root.get("code")).value(employeeSubquery),
                        cb.in(root.get("code")).value(managerSubquery)
                );
            });
        }

        if (gender != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), likePattern),
                    cb.like(cb.lower(root.get("lastName")), likePattern)
            ));
        }

        Page<Personnel> personnelPage = personnelRepository.findAll(spec, pageable);

        return PageResponse.<PersonnelResponse>builder()
                .currentPage(page)
                .pageSize(personnelPage.getSize())
                .totalPages(personnelPage.getTotalPages())
                .totalElements(personnelPage.getTotalElements())
                .data(personnelPage.getContent().stream()
                        .map(this::toPersonnelResponse)
                        .toList())
                .build();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
    public PersonnelResponse updateBasicSalary(String code, UpdateSalaryRequest request) {
        Personnel personnel = personnelRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

        boolean changed = false;

        if (request.getBasicSalary() != null) {
            personnel.setBasicSalary(request.getBasicSalary());
            changed = true;
        }
        if (request.getBonus() != null) {
            personnel.setBonus(request.getBonus());
            changed = true;
        }
        if (request.getAllowance() != null) {
            personnel.setAllowance(request.getAllowance());
            changed = true;
        }
        if (request.getKpiPenalty() != null) {
            personnel.setKpiPenalty(request.getKpiPenalty());
            changed = true;
        }

        personnel = personnelRepository.save(personnel);
        if (changed) salaryService.calculateSalary(personnel);

        return toPersonnelResponse(personnel);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
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
        PersonnelResponse response = personnelMapper.toPersonnelResponse(personnel);
        Role role = personnel.getAccount().getRole();

        switch (role) {
            case EMPLOYEE -> employeeRepository.findById(personnel.getCode()).ifPresent(employee -> {
                response.setDepartmentName(getDepartmentName(employee.getDepartment()));
                response.setDepartmentId(employee.getDepartment() == null ? null : employee.getDepartment().getId());
            });
            case MANAGER -> managerRepository.findById(personnel.getCode()).ifPresent(manager -> {
                response.setDepartmentName(getDepartmentName(manager.getDepartment()));
                response.setDepartmentId(manager.getDepartment() == null ? null : manager.getDepartment().getId());
            });
            case ADMIN -> response.setDepartmentName("Administration");
            default -> { /* do nothing */ }
        }

        return response;
    }

    private String getDepartmentName(Department department) {
        return department != null ? department.getName() : null;
    }
}
