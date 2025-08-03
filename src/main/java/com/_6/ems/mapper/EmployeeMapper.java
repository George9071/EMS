package com._6.ems.mapper;

import com._6.ems.dto.request.EmployeeCreationRequest;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.dto.response.EmployeeSimpleResponse;
import com._6.ems.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(source = "employee_code", target = "code")
    @Mapping(target = "informationRecord", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "uploadedFiles", ignore = true)
    Employee toEmployee(EmployeeCreationRequest request);

    @Mapping(source = "code", target = "employee_code")
    @Mapping(source = "department.id", target = "department_id")
    @Mapping(source = "taskCompleted", target = "task_completed")
    EmployeeResponse toEmployeeResponse(Employee employee);

    @Mapping(
            target = "fullName",
            expression = "java(employee.getInformationRecord().getFirstName() + \" \" + employee.getInformationRecord().getLastName())"
    )
    EmployeeSimpleResponse toSimpleResponse(Employee employee);

    List<EmployeeSimpleResponse> toSimpleResponseList(List<Employee> employees);
}
