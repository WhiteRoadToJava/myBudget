package com.mybudget.server.services;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.ExpenseRepository;
import com.mybudget.server.repositories.IncomseRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    private final IncomseRepository incomseRepository;
    private final ExpenseRepository expenseRepository;


    public AccountResponse createAccount (AccountRequest accountRequest){
        User currentUser  = userUtils.getCurrentAuthenticatedUser();
        if (findAccountByName(accountRequest.getName() , currentUser)){
            throw new ResourceNotFoundException("Account already exists");
        }
        Date createdAt = new Date();

        Account account = new Account(
                accountRequest.getName(),
                accountRequest.getDescription(),
                accountRequest.getBalance(),
                accountRequest.getTotalBalance(),
                accountRequest.getCurrency(),
                accountRequest.getType(),
                currentUser,
                createdAt.toString()
        );
        AccountResponse accountResponse = mapToAccountResponse(accountRepository.save(account));
        return accountResponse;
    }
    public AccountResponse updateAcoount(Account accouunt){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account existedAccount = accountRepository.findByIdAndUser(accouunt.getId(), currentUser);
        if(existedAccount == null){
            throw new ResourceNotFoundException("Account not found");
        }
        existedAccount.setName(accouunt.getName());
        existedAccount.setDescription(accouunt.getDescription());
        existedAccount.setCurrency(accouunt.getCurrency());
        existedAccount.setType(accouunt.getType());

        double oldBalance = existedAccount.getBalance();
        double newBslsnce = accouunt.getBalance();
        double oldTotalBalance = existedAccount.getTotalBalance();
        existedAccount.setTotalBalance(calculateNewTotal(oldBalance, newBslsnce, oldTotalBalance));
        existedAccount.setBalance(newBslsnce);
        return mapToAccountResponse(accountRepository.save(existedAccount));
    }
    private Double calculateNewTotal(Double oldBalance, Double newBalance, Double currentTotal) {

        return currentTotal + (newBalance - oldBalance);
    }


    public AllAccounts getAllAccounts (){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        List<Account> accounts = accountRepository.findAllByUser(currentUser);
        int totalAccounts = accounts.size();
        double totalBalance = 0.0;
        for (Account account : accounts) {
            if (account.getTotalBalance() == null) {
                account.setTotalBalance(0.0);
                accountRepository.save(account);
            }
            totalBalance += account.getTotalBalance();
        }
        AllAccounts allAccounts = new AllAccounts(accounts, totalAccounts, String.valueOf(totalBalance));
        return allAccounts;
    }

        public List<Transaction> getAllTransationInAccount(Account account){
            List<Incomse> incomseList = incomseRepository.findAllByAccount(account);
            List<Expense> expenseList = expenseRepository.findAllByAccount(account);
            List<Transaction> transactions = new ArrayList<>();
            transactions.addAll(incomseList.stream().map(this::mapIToTransation).toList());
            transactions.addAll(expenseList.stream().map(this::mapExpenseToTranaction).toList());
            return transactions;
        }




    private boolean findAccountByName(String accountName, User currentUser){
        return  accountRepository.findByNameAndUser(accountName, currentUser).isPresent();
    }
    public AccountResponse getAccountById(String accountId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if(account == null){
            throw new ResourceNotFoundException("Account not found");
        }
        return mapToAccountResponse(account);
    }



public Account updateTotalBalanaceWithExpense(Account account, double amount ){
        account.setTotalBalance(account.getTotalBalance() - amount);
        return accountRepository.save(account);
}

public Account updateTotalBalanceWithIncome(Account account, double amount ){
        if(account.getTotalBalance() == null){
            account.setTotalBalance(0.0);
            accountRepository.save(account);
        }
        if(account.getBalance() == null){
            throw new ResourceNotFoundException("Balance is null");
        }
        account.setTotalBalance(account.getTotalBalance() + amount);
        return accountRepository.save(account);
}
@Transactional
public Account updateAccountWithTransfer(Account fromAccount, Account toAccount, double amount){
        fromAccount.setTotalBalance(fromAccount.getTotalBalance() - amount);
        toAccount.setTotalBalance(toAccount.getTotalBalance() + amount);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        return fromAccount;
}



    private AccountResponse mapToAccountResponse(Account account) {
        String date = account.getCreatedAt();
        if(date == null) date = "2010-10-10";
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getCurrency(),
                account.getType(),
                account.getBalance(),
                account.getTotalBalance(),
                date
        );
    }
    private Transaction mapIToTransation (Incomse incomse){
        String type = "type";
        if(incomse.getType() == null){
            type= "incomse";
        }else type = incomse.getType();

        return new Transaction(
                incomse.getId(),
                incomse.getAmount(),
                incomse.getCategory(),
                incomse.getAccount(),
                type,
                incomse.getCreatedAt()
        );
    }
    private Transaction mapExpenseToTranaction(Expense expense) {
        String type = "type";
        if (expense.getType() == null) {
            type = "expense";
        } else type = expense.getType();
        LocalDateTime date = expense.getCreatedAt();
        return new Transaction(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getAccount(),
                type,
                date
        );
    }
}
