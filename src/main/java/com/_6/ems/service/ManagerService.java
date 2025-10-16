package com._6.ems.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com._6.ems.dto.request.ManagerCreationRequest;
import com._6.ems.entity.Account;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.ManagerMapper;
import com._6.ems.repository.AccountRepository;
import com._6.ems.repository.PersonnelRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Manager;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.ManagerRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerService {

    ManagerRepository managerRepository;
    DepartmentRepository departmentRepository;
    PersonnelRepository personnelRepository;
    ManagerMapper managerMapper;

    AccountRepository accountRepository;

    @Transactional
    public ManagerResponse createManager(ManagerCreationRequest request) {
        Personnel personnel = personnelRepository.findById(request.getCode())
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        Account account = personnel.getAccount();
        account.setRole(Role.MANAGER);
        accountRepository.save(account);

        log.info("Account id: {}, Role: {}", account.getId(), account.getRole());

        Department department = departmentRepository.findById(Integer.parseInt(request.getDepartment_id()))
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        if (department.getManager() != null) {
            throw new AppException(ErrorCode.DEPARTMENT_ALREADY_ASSIGNED);
        }

        Manager manager = Manager.builder()
                .informationRecord(personnel)
                .department(department)
                .manageDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        try {
            manager = managerRepository.save(manager);
            log.info("Manager code: {}", manager.getCode());
        } catch (Exception e) {
            log.error("Failed to save manager: {}", e.getMessage(), e);
            throw e;
        }

        department.setManager(manager);
        log.info("Department id: {}, manager code: {}", department.getId(), department.getManager().getCode());

        return managerMapper.toManagerResponse(manager);
    }

    public ManagerResponse getManagerByCode(String code) {
        Manager manager = managerRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        return managerMapper.toManagerResponse(manager);
    }

    public List<ManagerResponse> getAllManagers() {
        return managerRepository.findAll().stream()
                .map(managerMapper::toManagerResponse)
                .toList();
    }
}

