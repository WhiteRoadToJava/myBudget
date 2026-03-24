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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    private final ExpenseRepository     expenseRepository;


    public ExpenseResponse addExpense(ExpenseRequset expenseRequest){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = new Expense();

        expense.setName(expenseRequest.getName());
        expense.setAmount(expenseRequest.getAmount());
        expense.setCategory(expenseRequest.getCategory());
        expense.setCreatedAt(expenseRequest.getCreatedAt());
        expense.setUser(currentUser);

        String accountId = expenseRequest.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        expense.setAccount(account);

        return mapToExpenseResponse(expenseRepository.save(expense));
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
                expense.getName(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getUser(),
                expense.getAccount()
        );
    }
}
