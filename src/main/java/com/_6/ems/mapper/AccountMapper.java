package com._6.ems.mapper;

import com._6.ems.dto.request.AccountCreationRequest;
import com._6.ems.dto.request.AccountUpdateRequest;
import com._6.ems.dto.response.AccountResponse;
import com._6.ems.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    Account toAccount(AccountCreationRequest request);

    AccountResponse toAccountResponse(Account account);

    void updateAccount(@MappingTarget Account account, AccountUpdateRequest request);
}
