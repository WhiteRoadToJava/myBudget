package com.mybudget.server.services;

import com.mybudget.server.dto.expense.ExpenseRequset;
import com.mybudget.server.dto.expense.ExpenseResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.ExpenseRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    private final ExpenseRepository     expenseRepository;
    private final  AccountService accountService;


    @Transactional
    public ExpenseResponse addExpense(ExpenseRequset expenseRequest){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = new Expense();
        expense.setAmount(expenseRequest.getAmount());
        expense.setCategory(expenseRequest.getCategory());
        expense.setUser(currentUser);

        String accountId = expenseRequest.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        expense.setAccount(account);
        accountService.updateTotalBalanaceWithExpense(account, expenseRequest.getAmount());

        return mapToExpenseResponse(expenseRepository.save(expense));
    }
    // this method is used with schedule
    @Transactional
    public ExpenseResponse addExpense(ExpenseRequset requset, User user){
        Expense expense = new Expense();
        expense.setAmount(requset.getAmount());
        expense.setCategory(requset.getCategory());
        expense.setUser(user);

        String accountId = requset.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, user);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        expense.setAccount(account);
        accountService.updateTotalBalanaceWithExpense(account, requset.getAmount());

        return mapToExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(String expenseId, ExpenseRequset expenseRequest){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        String accountId = expenseRequest.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        accountService.updateTotalBalanceWithUpdateExpense(account, expense, expenseRequest.getAmount());
        expense.setAccount(account);
        if(expense.getUser().getId().equals(currentUser.getId())){
            expense.setAmount(expenseRequest.getAmount());
            expense.setCategory(expenseRequest.getCategory());
        }
        return mapToExpenseResponse(expenseRepository.save(expense));
    }
    @Transactional
public String  deleteExpense(String expenseId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if(expense.getUser().getId().equals(currentUser.getId())){
            accountService.updateTotalBalanceWithDeleteExpense(expense.getAccount(), expense.getAmount());
            expenseRepository.delete(expense);
            return "Expense deleted successfully";
        } else {
            throw new ResourceNotFoundException("Expense not found or access denied");
        }
}

    public ExpenseResponse getExpenseById(String expenseId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found or access denied"));

        if(!expense.getUser().getId().equals(currentUser.getId())){
            throw new ResourceNotFoundException("Expense not found or access denied");
        } else {
            return mapToExpenseResponse(expense);
        }
    }
    public List<ExpenseResponse> getAllExpensesByUser(){
        User user = userUtils.getCurrentAuthenticatedUser();
        List<Expense> expenses = expenseRepository.findAllByUser(user);
        return expenses.stream().map(this::mapToExpenseResponse).toList();
    }

    public List<ExpenseResponse> getAllExpensesByAccount(Account account){
        List<Expense> expenses = expenseRepository.findAllByAccount(account);
        return expenses.stream().map(this::mapToExpenseResponse).toList();
    }



    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getUser(),
                expense.getAccount()
        );
    }
}
