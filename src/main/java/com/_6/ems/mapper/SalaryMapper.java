package com._6.ems.mapper;


import com._6.ems.dto.response.SalaryResponse;
import com._6.ems.entity.Salary;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalaryMapper {

    @Mapping(target = "personnel_code", source = "owner.code")
    @Mapping(target = "month", source = "month")
    @Mapping(target = "year", source = "year")
    @Mapping(target = "bonus", source = "bonus")
    @Mapping(target = "penalty", source = "penalty")
    @Mapping(target = "real_pay", source = "realPay")
    @Mapping(target = "half_day", source = "halfWork")
    @Mapping(target = "full_day", source = "fullWork")
    @Mapping(target = "absence", source = "absence")
    SalaryResponse toSalaryResponse(Salary salary);
}
