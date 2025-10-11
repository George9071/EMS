package com._6.ems.mapper;

import com._6.ems.dto.request.ManagerCreationRequest;
import com._6.ems.dto.response.ManagerResponse;
import com._6.ems.entity.Manager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManagerMapper {
    @Mapping(source = "code", target = "code")
    @Mapping(source = "manageDate", target = "manageDate")
    @Mapping(target = "name", expression = "java(manager.getInformationRecord().getFirstName() + \" \" + manager.getInformationRecord().getLastName())")
    @Mapping(source = "informationRecord.gender", target = "gender")
    @Mapping(source = "informationRecord.email", target = "email")
    @Mapping(source = "informationRecord.phoneNumber", target = "phone")
    @Mapping(source = "informationRecord.avatar", target = "avatar")
    ManagerResponse toManagerResponse(Manager manager);
}
