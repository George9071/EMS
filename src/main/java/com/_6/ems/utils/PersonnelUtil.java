package com._6.ems.utils;

import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Personnel;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.record.PersonnelInfor;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.PersonnelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PersonnelUtil {

    private final PersonnelRepository personnelRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public Personnel getCurrentPersonnel() {
        return personnelRepository.findByCode(SecurityUtil.getCurrentUserCode())
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    }

    public PersonnelInfor getPersonnelInforByCode(String code) {
        Personnel personnel = getCurrentPersonnel();

        String fullName = personnel.getLastName() + " " + personnel.getFirstName();

        String dateOfBirth = String.format(personnel.getDob().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        Employee employee = employeeRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
        Department department = departmentRepository.findById(employee.getDepartment().getId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        String departmentName = department.getName();

        String position = personnel.getPosition();

        return new PersonnelInfor(
                fullName,
                dateOfBirth,
                personnel.getEmail(),
                personnel.getPhoneNumber(),
                personnel.getCity(),
                personnel.getStreet(),
                departmentName,
                position
        );
    }
}
