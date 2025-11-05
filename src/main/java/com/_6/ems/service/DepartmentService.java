package com._6.ems.service;

import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.request.DepartmentUpdateRequest;
import com._6.ems.dto.response.PageMeta;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.DepartmentMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(DepartmentCreationRequest request) {
        Department dept = departmentMapper.toDepartment(request);
        Department savedDept = departmentRepository.save(dept);

        String managerId = request.getManagerId();
        if (managerId == null || managerId.isBlank()) return departmentMapper.toDepartmentResponse(savedDept);

        Manager manager = managerRepository.findById(request.getManagerId())
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        Department existingDept = manager.getDepartment();
        if (existingDept != null) throw new AppException(ErrorCode.MANAGER_ALREADY_ASSIGNED);

        manager.setDepartment(savedDept);
        savedDept.setManager(manager);
        managerRepository.save(manager);

        return departmentMapper.toDepartmentResponse(savedDept);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse getDepartmentById(int id) {
        Department department = departmentRepository.findByIdWithManagerDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAllWithManagerDetails()
                .stream()
                .map(departmentMapper::toDepartmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageMeta<DepartmentResponse> getAllDepartments(Pageable pageable) {
        Page<Integer> departmentIds= departmentRepository.findAllDepartmentIds(pageable);

        List<Department> departments = departmentIds.isEmpty()
                ? List.of()
                : departmentRepository.findAllByIdsWithManagerDetails(departmentIds.getContent());

        List<DepartmentResponse> content = departments.stream()
                .map(departmentMapper::toDepartmentResponse)
                .toList();

        String sortBy = "id";
        String sortDir = "asc";

        if (pageable.getSort().isSorted()) {
            List<Sort.Order> orders = pageable.getSort().stream().toList();

            sortBy = orders.stream()
                    .map(Sort.Order::getProperty)
                    .collect(Collectors.joining(","));

            sortDir = orders.stream()
                    .map(order -> order.getDirection().isAscending() ? "asc" : "desc")
                    .collect(Collectors.joining(","));
        }

        return PageMeta.<DepartmentResponse>builder()
                .content(content)
                .totalElements(departmentIds.getTotalElements())
                .totalPages(departmentIds.getTotalPages())
                .numberOfElements(departmentIds.getNumberOfElements())
                .page(departmentIds.getNumber())
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public EmployeeInDepartmentResponse getAllEmployeesInDepartment(int departmentId) {
        Department department = departmentRepository.findByIdWithDetails(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return departmentMapper.toEmployeeInDepartment(department);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse assignManagerToDepartment(int departmentId, String managerId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        // Idempotent: same manager already assigned to this department -> do nothing
        if (department.getManager() != null &&
                department.getManager().getCode().equals(managerId)) {
            return departmentMapper.toDepartmentResponse(department);
        }

        // If department already has a different manager -> reject
        if (department.getManager() != null) throw new AppException(ErrorCode.DEPARTMENT_ALREADY_HAS_MANAGER);

        // If manager currently manages a different department, break that link.
        Department olDept = manager.getDepartment();
        if (olDept != null && olDept.getId() != departmentId) olDept.unassignManager();

        // Assign to the new department
        department.assignManager(manager);

        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public DepartmentResponse removeManagerFromDepartment(int departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (department.getManager() == null) return departmentMapper.toDepartmentResponse(department);
        department.unassignManager();
        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public EmployeeInDepartmentResponse assignEmployeeToDepartment(int departmentId, String code) {
        Department target = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department current = employee.getDepartment();

        // Already in this department then no-op
        if (current != null && current.getId() == target.getId()) {
            return departmentMapper.toEmployeeInDepartment(target);
        }

        if (current != null)
            throw new AppException(ErrorCode.EMPLOYEE_ALREADY_ASSIGNED_TO_ANOTHER_DEPARTMENT);

        target.addEmployee(employee);

        return departmentMapper.toEmployeeInDepartment(target);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public void removeEmployeeFromDepartment(int departmentId, String code) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Employee employee = employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department current = employee.getDepartment();
        if (current == null || current.getId() != dept.getId()) throw new AppException(ErrorCode.EMPLOYEE_NOT_BELONG);

        current.removeEmployee(employee);
    }

    @Transactional
    public void deleteDepartment(int departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Unassign manager
        if (department.getManager() != null) department.unassignManager();

        // Detach all employees
        for (Employee e : department.getEmployees()) department.removeEmployee(e);

        departmentRepository.delete(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(int departmentId, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        departmentMapper.updateDepartment(department, request);
        return departmentMapper.toDepartmentResponse(department);
    }
}
