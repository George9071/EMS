package com._6.ems.service;

import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.entity.Personnel;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.DepartmentMapper;
import com._6.ems.repository.PersonnelRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.dto.response.EmployeeInDepartmentResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Manager;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ManagerRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DepartmentService {
    DepartmentRepository departmentRepository;

    EmployeeRepository employeeRepository;
    ManagerRepository managerRepository;
    DepartmentMapper departmentMapper;
    PersonnelRepository personnelRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreationRequest request) {
        Department department = departmentMapper.toDepartment(request);

        if (request.getManager_id() != null) {
            Manager manager = managerRepository.findById(request.getManager_id())
                    .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

            Department previousDept = manager.getDepartment();
            if (previousDept != null) {
                previousDept.setManager(null);
                departmentRepository.save(previousDept);
            }
            department.setManager(manager);
            manager.setDepartment(department);
            managerRepository.save(manager);
        }

        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    public DepartmentResponse getDepartmentById(int id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        return departmentMapper.toDepartmentResponse(department);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDepartmentResponse)
                .collect(Collectors.toList());
    }

    public EmployeeInDepartmentResponse getAllEmployeesInDepartment(int departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return departmentMapper.toEmployeeInDepartment(department);
    }

    @Transactional
    public DepartmentResponse assignManagerToDepartment(int departmentId, String managerId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        Manager currentManager = department.getManager();
        if (currentManager != null && !currentManager.getCode().equals(managerId)) {
            currentManager.setDepartment(null);
            managerRepository.save(currentManager);
        }

        // Unassign the manager from their previous department, if needed
        Department previousDept = manager.getDepartment();
        if (previousDept != null && previousDept.getId() != departmentId) {
            previousDept.setManager(null);
            departmentRepository.save(previousDept);
        }

        // Set the new assignment
        department.setManager(manager);
        manager.setDepartment(department);

        // Save both sides of the relationship
        managerRepository.save(manager);
        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse removeManagerFromDepartment(int departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Manager manager = department.getManager();
        if (manager != null) {
            manager.setDepartment(null);
            managerRepository.save(manager);
        }

        department.setManager(null);
        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public EmployeeInDepartmentResponse assignEmployeeToDepartment(int departmentId, String code) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Personnel record = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        Employee employee = employeeRepository.findById(code).orElse(null);

        if (employee == null) {
            employee = Employee.builder()
                    .informationRecord(record)
                    .department(department)
                    .build();

            employee = employeeRepository.save(employee);

            department.getEmployees().add(employee);
            department.setEmployeeNumber(department.getEmployees().size());
        } else {
            Department currentDept = employee.getDepartment();

            if (currentDept != null && currentDept.getId() != departmentId) {
                currentDept.getEmployees().remove(employee);
                currentDept.setEmployeeNumber(currentDept.getEmployees().size());
            }

            employee.setDepartment(department);
            department.setEmployeeNumber(department.getEmployeeNumber() + 1);

            if (currentDept == null || currentDept.getId() != departmentId) {
                employee.setDepartment(department);
                employeeRepository.save(employee);

                department.getEmployees().add(employee);
                department.setEmployeeNumber(department.getEmployees().size());
            }
        }

        return departmentMapper.toEmployeeInDepartment(departmentRepository.save(department));
    }

    @Transactional
    public void removeEmployeeFromDepartment(int departmentId, String code) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department currentDept = employee.getDepartment();

        if (currentDept == null || currentDept.getId() != departmentId) {
            throw new AppException(ErrorCode.EMPLOYEE_NOT_BELONG);
        }

        employee.setDepartment(null);
        employeeRepository.save(employee);

        List<Employee> employeeList = department.getEmployees();
        if (employeeList != null) {
            employeeList.removeIf(e -> e.getCode().equals(code));
            department.setEmployeeNumber(employeeList.size());
        } else {
            department.setEmployeeNumber(0);
        }

        departmentRepository.save(department);
    }

}
