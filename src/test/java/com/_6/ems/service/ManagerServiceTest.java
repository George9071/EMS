package com._6.ems.service;

import com._6.ems.dto.request.ManagerCreationRequest;
import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.entity.Account;
import com._6.ems.entity.Department;
import com._6.ems.entity.Manager;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.ManagerMapper;
import com._6.ems.repository.AccountRepository;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.ManagerRepository;
import com._6.ems.repository.PersonnelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private ManagerMapper managerMapper;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ManagerService managerService;

    private Personnel personnel;
    private Account account;
    private Department department;
    private Manager manager;
    private ManagerCreationRequest creationRequest;
    private ManagerResponse managerResponse;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id("ACC001")
                .username("testmanager")
                .role(Role.EMPLOYEE)
                .build();

        personnel = Personnel.builder()
                .code("MGR001")
                .firstName("Jane")
                .lastName("Smith")
                .account(account)
                .build();

        department = Department.builder()
                .id(1)
                .name("HR Department")
                .manager(null) // Ensure no existing manager
                .build();

        manager = Manager.builder()
                .code("MGR001")
                .informationRecord(personnel)
                .department(department)
                .manageDate(LocalDate.now())
                .build();

        creationRequest = ManagerCreationRequest.builder()
                .code("MGR001")
                .department_id("1")
                .build();

        managerResponse = ManagerResponse.builder()
                .code("MGR001")
                .build();
    }

    @Test
    void createManager_validRequest_success() {
        // GIVEN
        // Create a fresh department without any manager assigned
        Department freshDepartment = Department.builder()
                .id(1)
                .name("HR Department")
                .build();
        
        when(personnelRepository.findById("MGR001")).thenReturn(Optional.of(personnel));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(departmentRepository.findById(1)).thenReturn(Optional.of(freshDepartment));
        when(managerRepository.save(any(Manager.class))).thenReturn(manager);
        when(managerMapper.toManagerResponse(manager)).thenReturn(managerResponse);

        // WHEN
        ManagerResponse response = managerService.createManager(creationRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("MGR001");
        assertThat(account.getRole()).isEqualTo(Role.MANAGER);
        verify(personnelRepository).findById("MGR001");
        verify(accountRepository).save(account);
        verify(departmentRepository).findById(1);
        verify(managerRepository).save(any(Manager.class));
    }

    @Test
    void createManager_personnelNotFound_throwsException() {
        // GIVEN
        when(personnelRepository.findById("MGR001")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> managerService.createManager(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_EXISTED);
        verify(personnelRepository).findById("MGR001");
        verify(managerRepository, never()).save(any());
    }

    @Test
    void createManager_departmentNotFound_throwsException() {
        // GIVEN
        when(personnelRepository.findById("MGR001")).thenReturn(Optional.of(personnel));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(departmentRepository.findById(1)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> managerService.createManager(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MANAGER_NOT_FOUND);
        verify(personnelRepository).findById("MGR001");
        verify(departmentRepository).findById(1);
        verify(managerRepository, never()).save(any());
    }

    @Test
    void createManager_departmentAlreadyHasManager_throwsException() {
        // GIVEN
        Manager existingManager = Manager.builder().code("MGR999").build();
        department.setManager(existingManager);

        when(personnelRepository.findById("MGR001")).thenReturn(Optional.of(personnel));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> managerService.createManager(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_ALREADY_HAS_MANAGER);
        verify(personnelRepository).findById("MGR001");
        verify(departmentRepository).findById(1);
        verify(managerRepository, never()).save(any());
    }

    @Test
    void getManagerByCode_validCode_success() {
        // GIVEN
        when(managerRepository.findById("MGR001")).thenReturn(Optional.of(manager));
        when(managerMapper.toManagerResponse(manager)).thenReturn(managerResponse);

        // WHEN
        ManagerResponse response = managerService.getManagerByCode("MGR001");

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("MGR001");
        verify(managerRepository).findById("MGR001");
    }

    @Test
    void getManagerByCode_invalidCode_throwsException() {
        // GIVEN
        when(managerRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> managerService.getManagerByCode("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MANAGER_NOT_FOUND);
        verify(managerRepository).findById("INVALID");
    }

    @Test
    void getAllManagers_success() {
        // GIVEN
        Manager manager2 = Manager.builder()
                .code("MGR002")
                .informationRecord(personnel)
                .build();
        ManagerResponse response2 = ManagerResponse.builder()
                .code("MGR002")
                .build();

        List<Manager> managers = Arrays.asList(manager, manager2);

        when(managerRepository.findAll()).thenReturn(managers);
        when(managerMapper.toManagerResponse(manager)).thenReturn(managerResponse);
        when(managerMapper.toManagerResponse(manager2)).thenReturn(response2);

        // WHEN
        List<ManagerResponse> result = managerService.getAllManagers();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("MGR001");
        assertThat(result.get(1).getCode()).isEqualTo("MGR002");
        verify(managerRepository).findAll();
        verify(managerMapper, times(2)).toManagerResponse(any(Manager.class));
    }
}
