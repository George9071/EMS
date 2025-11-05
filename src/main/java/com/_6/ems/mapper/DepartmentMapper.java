package com._6.ems.mapper;

import com._6.ems.dto.request.DepartmentCreationRequest;
import com._6.ems.dto.request.DepartmentUpdateRequest;
import com._6.ems.dto.response.DepartmentResponse;
import com._6.ems.dto.response.EmployeeInDepartmentResponse;
import com._6.ems.entity.Department;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class, ManagerMapper.class})
public interface DepartmentMapper {

    @Mapping(source = "id", target = "departmentId")
    @Mapping(source = "name", target = "departmentName")
    @Mapping(source = "manager", target = "manager")
    @Mapping(source = "employees", target = "employees")
    EmployeeInDepartmentResponse toEmployeeInDepartment(Department department);

    @Mapping(source = "id", target = "departmentId")
    @Mapping(source = "name", target = "departmentName")
    @Mapping(source = "employeeNumber", target = "employeeNumber")
    @Mapping(source = "establishmentDate", target = "establishmentDate")
    @Mapping(source = "manager", target = "manager")
    DepartmentResponse toDepartmentResponse(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeNumber", constant = "0")
    @Mapping(target = "establishmentDate", expression = "java(java.time.LocalDate.now(java.time.ZoneId.of(\"Asia/Ho_Chi_Minh\")))")
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "manager", ignore = true)
    Department toDepartment(DepartmentCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDepartment(@MappingTarget Department department, DepartmentUpdateRequest request);
}
