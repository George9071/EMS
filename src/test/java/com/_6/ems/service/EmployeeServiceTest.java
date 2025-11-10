package com._6.ems.service;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.EmployeeMapper;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private Department department;
    private Employee employee;
    private EmployeeCreationRequest creationRequest;
    private EmployeeResponse employeeResponse;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1)
                .name("IT Department")
                .build();

        employee = Employee.builder()
                .code("EMP001")
                .department(department)
                .build();

        creationRequest = EmployeeCreationRequest.builder()
                .department_id(1)
                .build();

        employeeResponse = EmployeeResponse.builder()
                .employee_code("EMP001")
                .department_id(1)
                .build();
    }

    @Test
    void createEmployee_validRequest_success() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        when(employeeMapper.toEmployee(creationRequest)).thenReturn(employee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toEmployeeResponse(employee)).thenReturn(employeeResponse);

        // WHEN
        EmployeeResponse result = employeeService.createEmployee(creationRequest);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getEmployee_code()).isEqualTo("EMP001");
        verify(departmentRepository).findById(1);
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeMapper).toEmployeeResponse(employee);
    }

    @Test
    void createEmployee_departmentNotFound_throwsException() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> employeeService.createEmployee(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(departmentRepository).findById(1);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getEmployeeByCode_validCode_success() {
        // GIVEN
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(employeeMapper.toEmployeeResponse(employee)).thenReturn(employeeResponse);

        // WHEN
        EmployeeResponse result = employeeService.getEmployeeByCode("EMP001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getEmployee_code()).isEqualTo("EMP001");
        verify(employeeRepository).findById("EMP001");
        verify(employeeMapper).toEmployeeResponse(employee);
    }

    @Test
    void getEmployeeByCode_invalidCode_throwsException() {
        // GIVEN
        when(employeeRepository.findById(anyString())).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> employeeService.getEmployeeByCode("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verify(employeeRepository).findById("INVALID");
    }

    @Test
    void getAllEmployee_success() {
        // GIVEN
        Employee employee2 = Employee.builder()
                .code("EMP002")
                .department(department)
                .build();
        EmployeeResponse response2 = EmployeeResponse.builder()
                .employee_code("EMP002")
                .department_id(1)
                .build();

        List<Employee> employees = Arrays.asList(employee, employee2);

        when(employeeRepository.findAll()).thenReturn(employees);
        when(employeeMapper.toEmployeeResponse(employee)).thenReturn(employeeResponse);
        when(employeeMapper.toEmployeeResponse(employee2)).thenReturn(response2);

        // WHEN
        List<EmployeeResponse> result = employeeService.getAllEmployee();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmployee_code()).isEqualTo("EMP001");
        assertThat(result.get(1).getEmployee_code()).isEqualTo("EMP002");
        verify(employeeRepository).findAll();
        verify(employeeMapper, times(2)).toEmployeeResponse(any(Employee.class));
    }
}
