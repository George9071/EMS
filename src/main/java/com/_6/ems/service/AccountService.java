package com._6.ems.service;

import java.util.List;

import com._6.ems.dto.response.AccountResponse;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com._6.ems.dto.request.AccountCreationRequest;
import com._6.ems.dto.request.AccountUpdateRequest;
import com._6.ems.entity.Account;
import com._6.ems.mapper.AccountMapper;
import com._6.ems.repository.AccountRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

    AccountRepository accountRepository;
    AccountMapper accountMapper;
    PasswordEncoder passwordEncoder;
    
    public Account createAccount(AccountCreationRequest request) {
        if (accountRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        Account account = accountMapper.toAccount(request);
        account.setRole(request.getRole());
        account.setPassword(passwordEncoder.encode(request.getPassword()));

        return accountRepository.save(account);
    }

    public Account updateAccount(String accountId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        accountMapper.updateAccount(account, request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));

        return accountRepository.save(account);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    @PostAuthorize("returnObject.username == authentication.name or hasRole('ADMIN')")
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));
        return accountMapper.toAccountResponse(account);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUTHORIZE_ADMIN')")
    public void deleteAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        accountRepository.delete(account);
    }
}
