package com._6.ems.mapper;

import com._6.ems.dto.request.SalaryUpdateRequest;
import com._6.ems.dto.response.SalaryDetailResponse;
import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.entity.Personnel;
import com._6.ems.entity.Salary;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalaryMapper {

    @Mapping(source = "personnel.code", target = "personnelCode")
    @Mapping(target = "personnelName", expression = "java(getFullName(salary.getPersonnel()))")
    SalaryResponse toResponse(Salary salary);

    @Mapping(source = "personnel.code", target = "personnelCode")
    @Mapping(target = "personnelName", expression = "java(getFullName(salary.getPersonnel()))")
    @Mapping(source = "personnel.position", target = "position")
    SalaryDetailResponse toDetailResponse(Salary salary);

    default String getFullName(Personnel personnel) {
        if (personnel == null) return "";
        String firstName = personnel.getFirstName() != null ? personnel.getFirstName() : "";
        String lastName = personnel.getLastName() != null ? personnel.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "personnel", ignore = true)
    @Mapping(target = "month", ignore = true)
    @Mapping(target = "year", ignore = true)
    @Mapping(target = "socialInsurance", ignore = true)
    @Mapping(target = "healthInsurance", ignore = true)
    @Mapping(target = "unemploymentInsurance", ignore = true)
    @Mapping(target = "personalIncomeTax", ignore = true)
    @Mapping(target = "grossSalary", ignore = true)
    @Mapping(target = "totalDeductions", ignore = true)
    @Mapping(target = "netSalary", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Salary salary, SalaryUpdateRequest request);
}