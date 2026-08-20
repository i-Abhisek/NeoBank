package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        log.info("Creating account for : {}", createAccountRequest.getEmail());

        if(accountRepository.existsByEmail(createAccountRequest.getEmail())){
            throw new RuntimeException("Account already exists for email: "+createAccountRequest.getEmail());
        }
        Account account = new Account();
        account.setAccountHolderName(createAccountRequest.getAccountHolderName());
        account.setEmail(createAccountRequest.getEmail());
        account.setPhone(createAccountRequest.getPhone());
        account.setAccountType(createAccountRequest.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(createAccountRequest.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                createAccountRequest.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("100000")
                : new BigDecimal("500000")
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);

    }

    private AccountResponse mapToResponse(Account account){
        AccountResponse response = new AccountResponse();
        response.setId(account.);
    }
}
