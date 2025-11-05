package com._6.ems.mapper;

import com._6.ems.dto.request.PersonnelCreationRequest;
import com._6.ems.dto.request.PersonnelUpdateRequest;
import com._6.ems.dto.response.PersonnelResponse;
import com._6.ems.entity.Personnel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonnelMapper {
    Personnel toPersonnel(PersonnelCreationRequest request);

    @Mappings({
            @Mapping(target = "avatarUrl", source = "avatar"),
            @Mapping(target = "accountId", source = "account.id"),
            @Mapping(target = "gender", source = "gender"),
            @Mapping(target = "role", source = "account.role")
    })
    PersonnelResponse toPersonnelResponse(Personnel personnel);

    @Mapping(target = "privileges", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePersonnel(@MappingTarget Personnel personnel, PersonnelUpdateRequest request);
}
