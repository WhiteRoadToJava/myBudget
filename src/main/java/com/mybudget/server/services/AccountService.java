package com.mybudget.server.services;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.util.UserUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service

public class AccountService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    public AccountService(UserUtils userUtils, AccountRepository accountRepository) {
        this.userUtils = userUtils;
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount (AccountRequest accountRequest){
        User currentUser  = userUtils.getCurrentAuthenticatedUser();
        if (findAccountByName(accountRequest.getName() , currentUser)){
            throw new IllegalArgumentException("Account already exists");
        }
        Account account = new Account(
                accountRequest.getName(),
                accountRequest.getDescription(),
                accountRequest.getBalance(),
                accountRequest.getCurrency(),
                accountRequest.getType(),
                currentUser
        );

        AccountResponse accountResponse = mapToAccountResponse(accountRepository.save(account));
        return accountResponse;
    }


    public AllAccounts getAllAccounts (){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        AllAccounts allAccounts = new AllAccounts();
        List<Account> accounts = accountRepository.findAllByUser(currentUser);
        allAccounts.setAccounts(accounts);
        AllAccounts.AccountsInfo accountsInfo = new AllAccounts.AccountsInfo();
        accountsInfo.setTotalAccounts(accounts.size());
        accountsInfo.setTotalBalance(
                String.valueOf(accounts.stream()
                        .mapToDouble(account -> account.getBalance() + account.getTotalBalance())
                        .sum()));
        allAccounts.setAccountsInfo(accountsInfo);

        return allAccounts;
    }

    private boolean findAccountByName(String accountName, User currentUser){
        return  accountRepository.findByNameAndUser(accountName, currentUser).isPresent();
    }




    private AccountResponse mapToAccountResponse(Account account) {
        AccountResponse accountResponse = new AccountResponse();
        AccountResponse.AccountInfo accountInfo = new AccountResponse.AccountInfo();
        accountResponse.setAllTransactions(new ArrayList());
        accountInfo.setName(account.getName());
        accountInfo.setCurrency(account.getCurrency());
        accountInfo.setBalance(account.getBalance());
        accountInfo.setType(account.getType());

        accountResponse.setAccountInfo(accountInfo);
        return accountResponse;
    }

}
