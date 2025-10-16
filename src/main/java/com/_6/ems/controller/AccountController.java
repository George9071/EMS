package com._6.ems.controller;

import java.util.List;

import com._6.ems.dto.response.AccountResponse;
import com._6.ems.mapper.AccountMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com._6.ems.dto.request.AccountUpdateRequest;
import com._6.ems.dto.response.ApiResponse;
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
    @Operation(
        summary = "Get all accounts",
        description = "Retrieve a list of all existing accounts. Only admin can call this API."
    )
    ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("auth: {}", authentication);
        log.info("Username: {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        List<AccountResponse> result = accountService.getAccounts().stream()
                .map(accountMapper::toAccountResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<AccountResponse>>builder()
                .result(result)
                .build());
    }

    @GetMapping("/{accountId}")
    @Operation(
            summary = "Get account by ID",
            description = "Retrieve a single account by its unique ID."
    )
    ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable("accountId") String accountId){
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(
                ApiResponse.<AccountResponse>builder()
                .result(response)
                .build()
        );
    }

    @Operation(
            summary = "Update account details",
            description = "Update information of an existing account by ID",
            parameters = {
                    @Parameter(name = "accountId", description = "Unique ID of the account", required = true)
            }
    )
    @PutMapping("/{accountId}")
    ApiResponse<AccountResponse> updateAccount(@PathVariable String accountId, @RequestBody AccountUpdateRequest request){
        return ApiResponse.<AccountResponse>builder()
                .result(accountMapper.toAccountResponse(accountService.updateAccount(accountId, request)))
                .build();
    }

    @Operation(
            summary = "Delete account",
            description = "Delete an existing account by its ID.",
            parameters = {
                    @Parameter(name = "accountId", description = "Unique ID of the account to delete", required = true)
            }
    )
    @DeleteMapping("/{accountId}")
    ApiResponse<String> deleteAccount(@PathVariable String accountId){
        accountService.deleteAccount(accountId);
        return ApiResponse.<String>builder()
                .result("Account has been deleted")
                .build();
    }
}
