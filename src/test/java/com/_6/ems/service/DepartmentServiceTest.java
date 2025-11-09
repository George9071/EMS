package com._6.ems.service;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.request.DepartmentUpdateRequest;
import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Manager;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.DepartmentMapper;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private Manager manager;
    private Employee employee;
    private DepartmentCreationRequest creationRequest;
    private DepartmentResponse departmentResponse;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1)
                .name("IT Department")
                .employees(new HashSet<>())
                .build();

        manager = Manager.builder()
                .code("MGR001")
                .build();

        employee = Employee.builder()
                .code("EMP001")
                .build();

        creationRequest = DepartmentCreationRequest.builder()
                .name("IT Department")
                .managerId("MGR001")
                .build();

        departmentResponse = DepartmentResponse.builder()
                .departmentId(1)
                .departmentName("IT Department")
                .build();
    }

    @Test
    void createDepartment_withoutManager_success() {
        // GIVEN
        DepartmentCreationRequest requestWithoutManager = DepartmentCreationRequest.builder()
                .name("IT Department")
                .build();

        when(departmentMapper.toDepartment(requestWithoutManager)).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toDepartmentResponse(department)).thenReturn(departmentResponse);

        // WHEN
        DepartmentResponse result = departmentService.createDepartment(requestWithoutManager);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName()).isEqualTo("IT Department");
        verify(departmentRepository).save(any(Department.class));
        verify(managerRepository, never()).findById(anyString());
    }

    @Test
    void createDepartment_withManager_success() {
        // GIVEN
        when(departmentMapper.toDepartment(creationRequest)).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(managerRepository.findById("MGR001")).thenReturn(Optional.of(manager));
        when(departmentMapper.toDepartmentResponse(department)).thenReturn(departmentResponse);

        // WHEN
        DepartmentResponse result = departmentService.createDepartment(creationRequest);

        // THEN
        assertThat(result).isNotNull();
        verify(departmentRepository).save(any(Department.class));
        verify(managerRepository).findById("MGR001");
        verify(managerRepository).save(manager);
    }

    @Test
    void createDepartment_managerAlreadyAssigned_throwsException() {
        // GIVEN
        Department existingDept = Department.builder().id(2).name("HR Department").build();
        manager.setDepartment(existingDept);

        when(departmentMapper.toDepartment(creationRequest)).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(managerRepository.findById("MGR001")).thenReturn(Optional.of(manager));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> departmentService.createDepartment(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MANAGER_ALREADY_ASSIGNED);
        verify(managerRepository).findById("MGR001");
    }

    @Test
    void getDepartmentById_validId_success() {
        // GIVEN
        when(departmentRepository.findByIdWithManagerDetails(1)).thenReturn(Optional.of(department));
        when(departmentMapper.toDepartmentResponse(department)).thenReturn(departmentResponse);

        // WHEN
        DepartmentResponse result = departmentService.getDepartmentById(1);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName()).isEqualTo("IT Department");
        verify(departmentRepository).findByIdWithManagerDetails(1);
    }

    @Test
    void getDepartmentById_invalidId_throwsException() {
        // GIVEN
        when(departmentRepository.findByIdWithManagerDetails(999)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> departmentService.getDepartmentById(999));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(departmentRepository).findByIdWithManagerDetails(999);
    }

    @Test
    void assignEmployeeToDepartment_success() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(departmentMapper.toEmployeeInDepartment(department)).thenReturn(null);

        // WHEN
        departmentService.assignEmployeeToDepartment(1, "EMP001");

        // THEN
        verify(departmentRepository).findById(1);
        verify(employeeRepository).findById("EMP001");
        assertThat(department.getEmployees()).hasSize(1);
    }

    @Test
    void assignEmployeeToDepartment_employeeNotFound_throwsException() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        when(employeeRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> departmentService.assignEmployeeToDepartment(1, "INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verify(employeeRepository).findById("INVALID");
    }

    @Test
    void getAllDepartments_success() {
        // GIVEN
        Department dept2 = Department.builder().id(2).name("HR Department").build();
        List<Department> departments = Arrays.asList(department, dept2);
        DepartmentResponse response2 = DepartmentResponse.builder().departmentId(2).departmentName("HR Department").build();

        when(departmentRepository.findAllWithManagerDetails()).thenReturn(departments);
        when(departmentMapper.toDepartmentResponse(department)).thenReturn(departmentResponse);
        when(departmentMapper.toDepartmentResponse(dept2)).thenReturn(response2);

        // WHEN
        List<DepartmentResponse> result = departmentService.getAllDepartments();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(departmentRepository).findAllWithManagerDetails();
    }

    @Test
    void updateDepartment_success() {
        // GIVEN
        DepartmentUpdateRequest updateRequest = DepartmentUpdateRequest.builder()
                .name("Updated IT Department")
                .build();

        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        doNothing().when(departmentMapper).updateDepartment(department, updateRequest);
        when(departmentMapper.toDepartmentResponse(department)).thenReturn(departmentResponse);

        // WHEN
        DepartmentResponse result = departmentService.updateDepartment(1, updateRequest);

        // THEN
        assertThat(result).isNotNull();
        verify(departmentRepository).findById(1);
        verify(departmentMapper).updateDepartment(department, updateRequest);
    }

    @Test
    void deleteDepartment_success() {
        // GIVEN
        department.setManager(manager);
        department.getEmployees().add(employee);

        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        doNothing().when(departmentRepository).delete(department);

        // WHEN
        departmentService.deleteDepartment(1);

        // THEN
        verify(departmentRepository).findById(1);
        verify(departmentRepository).delete(department);
    }
}
