package com.mybudget.server.services;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;

    public AccountResponse createAccount (AccountRequest accountRequest){
        User currentUser  = userUtils.getCurentAuthenticatedUser();

        Account account = new Account(
                accountRequest.getName(),
                accountRequest.getDescription(),
                accountRequest.getBalance(),
                accountRequest.getCurrency(),
                accountRequest.getType(),
                currentUser
        );

        accountRepository.save(account);

        return new AccountResponse();
    }



}
