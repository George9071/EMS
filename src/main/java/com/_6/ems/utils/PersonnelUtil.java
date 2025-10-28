package com._6.ems.utils;

import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Personnel;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.record.PersonnelInfo;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.PersonnelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public PersonnelInfo getPersonnelInfoByCode(String code) {
        Personnel personnel = personnelRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

        String fullName = personnel.getLastName() + " " + personnel.getFirstName();
        String dateOfBirth = personnel.getDob().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Employee employee = employeeRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department department = departmentRepository.findById(employee.getDepartment().getId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        String departmentName = department.getName();
        String position = personnel.getPosition();

        return new PersonnelInfo(
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

    // Method mới - batch loading để tránh N+1 query
    public Map<String, PersonnelInfo> getPersonnelInfoByCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }

        // Batch load tất cả personnel
        List<Personnel> personnelList = personnelRepository.findByCodeIn(codes);

        // Batch load tất cả employee với department (sử dụng fetch join)
        List<Employee> employees = employeeRepository.findByCodeInWithDepartment(codes);

        // Tạo map để truy xuất nhanh
        Map<String, Personnel> personnelMap = personnelList.stream()
                .collect(Collectors.toMap(Personnel::getCode, Function.identity()));

        Map<String, Employee> employeeMap = employees.stream()
                .collect(Collectors.toMap(Employee::getCode, Function.identity()));

        // Build PersonnelInfo cho từng code
        return codes.stream()
                .filter(code -> personnelMap.containsKey(code) && employeeMap.containsKey(code))
                .collect(Collectors.toMap(
                        Function.identity(),
                        code -> buildPersonnelInfo(personnelMap.get(code), employeeMap.get(code))
                ));
    }

    private PersonnelInfo buildPersonnelInfo(Personnel personnel, Employee employee) {
        String fullName = personnel.getLastName() + " " + personnel.getFirstName();
        String dateOfBirth = personnel.getDob().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String departmentName = employee.getDepartment().getName();
        String position = personnel.getPosition();

        return new PersonnelInfo(
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
