package com._6.ems.service;

import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.EmployeeMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeService {

    EmployeeRepository employeeRepository;
    DepartmentRepository departmentRepository;
    EmployeeMapper employeeMapper;

    public EmployeeResponse createEmployee(EmployeeCreationRequest request) {
        Department department = departmentRepository.findById(request.getDepartment_id())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Employee employee = employeeMapper.toEmployee(request);

        employee.setDepartment(department);

        return employeeMapper.toEmployeeResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse getEmployeeByCode(String code) {
        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        return employeeMapper.toEmployeeResponse(employee);
    }

    public List<EmployeeResponse> getAllEmployee() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toEmployeeResponse)
                .collect(Collectors.toList());
    }
}
