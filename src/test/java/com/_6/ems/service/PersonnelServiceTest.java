package com._6.ems.service;

import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.request.UpdateSalaryRequest;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.entity.Account;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.Gender;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.PersonnelMapper;
import com._6.ems.repository.*;
import com._6.ems.utils.CloudinaryUtil;
import com._6.ems.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonnelServiceTest {

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private NotificationRecipientRepository notificationRecipientRepository;

    @Mock
    private PersonnelMapper personnelMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private SalaryService salaryService;

    @Mock
    private CloudinaryUtil cloudinaryUtil;

    @Mock
    private MeetingBookingRepository meetingBookingRepository;

    @InjectMocks
    private PersonnelService personnelService;

    private Personnel personnel;
    private Account account;
    private Employee employee;
    private PersonnelResponse personnelResponse;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id("ACC001")
                .username("testuser")
                .role(Role.EMPLOYEE)
                .build();

        personnel = Personnel.builder()
                .code("PER001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .dob(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .basicSalary(10000000.0)
                .account(account)
                .build();

        employee = Employee.builder()
                .code("PER001")
                .informationRecord(personnel)
                .build();

        personnelResponse = PersonnelResponse.builder()
                .code("PER001")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void getMyInfo_validUser_success() {
        // GIVEN
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
            when(personnelRepository.findByAccount_Id("ACC001")).thenReturn(Optional.of(personnel));
            when(personnelMapper.toPersonnelResponse(personnel)).thenReturn(personnelResponse);
            when(employeeRepository.findById("PER001")).thenReturn(Optional.of(employee));

            // Mock SecurityContext behavior directly
            org.springframework.security.core.context.SecurityContext securityContext = 
                mock(org.springframework.security.core.context.SecurityContext.class);
            org.springframework.security.core.Authentication authentication = 
                mock(org.springframework.security.core.Authentication.class);
            
            when(authentication.getName()).thenReturn("ACC001");
            when(securityContext.getAuthentication()).thenReturn(authentication);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

            // WHEN
            PersonnelResponse result = personnelService.getMyInfo();

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("PER001");
            verify(accountRepository).findById("ACC001");
            verify(personnelRepository).findByAccount_Id("ACC001");
        }
    }

    @Test
    void getPersonnelByCode_validCode_success() {
        // GIVEN
        when(personnelRepository.findById("PER001")).thenReturn(Optional.of(personnel));
        when(personnelMapper.toPersonnelResponse(personnel)).thenReturn(personnelResponse);
        when(employeeRepository.findById("PER001")).thenReturn(Optional.of(employee));

        // WHEN
        PersonnelResponse result = personnelService.getPersonnelByCode("PER001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PER001");
        verify(personnelRepository).findById("PER001");
    }

    @Test
    void getPersonnelByCode_invalidCode_throwsException() {
        // GIVEN
        when(personnelRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> personnelService.getPersonnelByCode("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_EXISTED);
        verify(personnelRepository).findById("INVALID");
    }

    @Test
    void updatePersonnel_validRequest_success() {
        // GIVEN
        PersonnelUpdateRequest updateRequest = PersonnelUpdateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();

        when(personnelRepository.findById("PER001")).thenReturn(Optional.of(personnel));
        doNothing().when(personnelMapper).updatePersonnel(personnel, updateRequest);
        when(personnelMapper.toPersonnelResponse(personnel)).thenReturn(personnelResponse);
        when(employeeRepository.findById("PER001")).thenReturn(Optional.of(employee));

        // WHEN
        PersonnelResponse result = personnelService.updatePersonnel("PER001", updateRequest);

        // THEN
        assertThat(result).isNotNull();
        verify(personnelRepository).findById("PER001");
        verify(personnelMapper).updatePersonnel(personnel, updateRequest);
    }

    @Test
    void updatePersonnel_personnelNotFound_throwsException() {
        // GIVEN
        PersonnelUpdateRequest updateRequest = PersonnelUpdateRequest.builder()
                .firstName("Jane")
                .build();

        when(personnelRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> personnelService.updatePersonnel("INVALID", updateRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_EXISTED);
        verify(personnelRepository).findById("INVALID");
    }

    @Test
    void updateBasicSalary_validRequest_success() {
        // GIVEN
    UpdateSalaryRequest updateRequest = new UpdateSalaryRequest();
    updateRequest.setBasicSalary(15000000.0);
    updateRequest.setBonus(1000000.0);
    updateRequest.setAllowance(500000.0);

        when(personnelRepository.findByCode("PER001")).thenReturn(Optional.of(personnel));
        when(personnelRepository.save(personnel)).thenReturn(personnel);
        doNothing().when(salaryService).calculateSalary(personnel);
        when(personnelMapper.toPersonnelResponse(personnel)).thenReturn(personnelResponse);
        when(employeeRepository.findById("PER001")).thenReturn(Optional.of(employee));

        // WHEN
        PersonnelResponse result = personnelService.updateBasicSalary("PER001", updateRequest);

        // THEN
        assertThat(result).isNotNull();
        assertThat(personnel.getBasicSalary()).isEqualTo(15000000.0);
        assertThat(personnel.getBonus()).isEqualTo(1000000.0);
        assertThat(personnel.getAllowance()).isEqualTo(500000.0);
        verify(personnelRepository).findByCode("PER001");
        verify(personnelRepository).save(personnel);
        verify(salaryService).calculateSalary(personnel);
    }

    @Test
    void updateBasicSalary_personnelNotFound_throwsException() {
        // GIVEN
    UpdateSalaryRequest updateRequest = new UpdateSalaryRequest();
    updateRequest.setBasicSalary(15000000.0);

        when(personnelRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> personnelService.updateBasicSalary("INVALID", updateRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERSONNEL_NOT_FOUND);
        verify(personnelRepository).findByCode("INVALID");
        verify(salaryService, never()).calculateSalary(any());
    }

    @Test
    void deletePersonnel_employeeWithDepartment_success() {
        // GIVEN
        when(personnelRepository.findById("PER001")).thenReturn(Optional.of(personnel));
        when(employeeRepository.findByCode("PER001")).thenReturn(Optional.of(employee));
        doNothing().when(meetingBookingRepository).deleteByOrganizerCode("PER001");
        doNothing().when(employeeRepository).delete(employee);
        doNothing().when(accountRepository).delete(account);
        doNothing().when(personnelRepository).delete(personnel);

        // WHEN
        personnelService.deletePersonnel("PER001");

        // THEN
        verify(personnelRepository).findById("PER001");
        verify(employeeRepository).findByCode("PER001");
        verify(meetingBookingRepository).deleteByOrganizerCode("PER001");
        verify(employeeRepository).delete(employee);
        verify(accountRepository).delete(account);
        verify(personnelRepository).delete(personnel);
    }

    @Test
    void deletePersonnel_personnelNotFound_throwsException() {
        // GIVEN
        when(personnelRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> personnelService.deletePersonnel("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERSONNEL_NOT_FOUND);
        verify(personnelRepository).findById("INVALID");
        verify(employeeRepository, never()).delete(any());
    }
}
