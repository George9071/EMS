package com._6.ems.mapper;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.dto.response.EmployeeInDepartmentResponse;
import com._6.ems.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class, ManagerMapper.class})
public interface DepartmentMapper {

    @Mapping(source = "id", target = "department_id")
    @Mapping(source = "name", target = "department_name")
    @Mapping(source = "manager", target = "manager")
    @Mapping(source = "employees", target = "employees")
    EmployeeInDepartmentResponse toEmployeeInDepartment(Department department);

    @Mapping(source = "id", target = "department_id")
    @Mapping(source = "name", target = "department_name")
    @Mapping(source = "employeeNumber", target = "employee_number")
    @Mapping(source = "establishmentDate", target = "establishment_date")
    @Mapping(source = "manager", target = "manager")
    DepartmentResponse toDepartmentResponse(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeNumber", constant = "0")
    @Mapping(target = "establishmentDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "manager", ignore = true)
    Department toDepartment(DepartmentCreationRequest request);
}
