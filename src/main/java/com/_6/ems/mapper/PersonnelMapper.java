package com._6.ems.mapper;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.entity.Personnel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonnelMapper {
    Personnel toPersonnel(PersonnelCreationRequest request);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "avatar", target = "avatarUrl")
    PersonnelResponse toPersonnelResponse(Personnel personnel);

    @Mapping(target = "privileges", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePersonnel(@MappingTarget Personnel personnel, PersonnelUpdateRequest request);
}
