package com._6.ems.controller;

import java.util.List;

import com._6.ems.dto.response.AccountResponse;
import com._6.ems.mapper.AccountMapper;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.AccountCreationRequest;
import com._6.ems.dto.request.AccountUpdateRequest;
import com._6.ems.dto.response.ApiResponse;
import com._6.ems.entity.Account;
import com._6.ems.service.AccountService;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {

    AccountService accountService;
    AccountMapper accountMapper;

//    @PostMapping
//    ApiResponse<AccountResponse> createAccount(@RequestBody @Valid AccountCreationRequest request){
//        log.info("Controller: create account");
//
//        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>();
//
//        apiResponse.setResult(accountMapper.toAccountResponse(accountService.createAccount(request)));
//
//        return apiResponse;
//    }

    @GetMapping
    ApiResponse<List<AccountResponse>> getAccounts(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("auth: {}", authentication);
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        List<AccountResponse> result = accountService.getAccounts().stream()
                .map(accountMapper::toAccountResponse)
                .toList();

        return ApiResponse.<List<AccountResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/{accountId}")
    ApiResponse<AccountResponse> getAccount(@PathVariable("accountId") String accountId){
        return ApiResponse.<AccountResponse>builder()
                .result(accountMapper.toAccountResponse(accountService.getAccount(accountId)))
                .build();
    }

    @PutMapping("/{accountId}")
    ApiResponse<AccountResponse> updateAccount(@PathVariable String accountId, @RequestBody AccountUpdateRequest request){
        return ApiResponse.<AccountResponse>builder()
                .result(accountMapper.toAccountResponse(accountService.updateAccount(accountId, request)))
                .build();
    }

    @DeleteMapping("/{accountId}")
    ApiResponse<String> deleteAccount(@PathVariable String accountId){
        accountService.deleteAccount(accountId);
        return ApiResponse.<String>builder()
                .result("Account has been deleted")
                .build();
    }
}
