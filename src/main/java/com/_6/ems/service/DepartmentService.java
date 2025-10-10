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
        Department dept = departmentMapper.toDepartment(request);
        Department savedDept = departmentRepository.save(dept); // must exist if Manager holds FK

        if (request.getManager_id() != null) {
            Manager manager = managerRepository.findById(request.getManager_id())
                    .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

            Department prev = manager.getDepartment();
            if (prev != null && prev.getId() != savedDept.getId()) {
                prev.setManager(null);
                manager.setDepartment(null);
            }

            // set new link (owning side = Manager)
            manager.setDepartment(savedDept);
            savedDept.setManager(manager);
            managerRepository.save(manager);
        }

        return departmentMapper.toDepartmentResponse(savedDept);
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

        if (department.getManager() != null && department.getManager().getCode().equals(managerId)) {
            return departmentMapper.toDepartmentResponse(department);
        }

        // If manager currently manages a different department, break that link.
        Department prev = manager.getDepartment();
        if (prev != null && prev.getId() != departmentId) {
            prev.unassignManager(); // updates both sides
        }

        // Assign new link (updates both sides)
        department.assignManager(manager);

        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse removeManagerFromDepartment(int departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (department.getManager() == null) return departmentMapper.toDepartmentResponse(department);
        department.unassignManager();
        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional
    public EmployeeInDepartmentResponse assignEmployeeToDepartment(int departmentId, String code) {
        Department target = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Personnel record = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        Employee employee = employeeRepository.findById(code).orElseGet(() -> {
            Employee e = Employee.builder()
                    .informationRecord(record)
                    .build();
            return employeeRepository.save(e);
        });

        Department current = employee.getDepartment();
        if (current != null && current.getId() == target.getId()) {
            return departmentMapper.toEmployeeInDepartment(target);
        }

        if (current != null) current.removeEmployee(employee);
        target.addEmployee(employee);

        return departmentMapper.toEmployeeInDepartment(target);
    }

    @Transactional
    public void removeEmployeeFromDepartment(int departmentId, String code) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department current = employee.getDepartment();
        if (current == null || current.getId() != departmentId) throw new AppException(ErrorCode.EMPLOYEE_NOT_BELONG);

        current.removeEmployee(employee);
    }

}
