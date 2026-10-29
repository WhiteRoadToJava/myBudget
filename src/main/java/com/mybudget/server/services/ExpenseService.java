package com.mybudget.server.services;

import com.mybudget.server.dto.expense.ExpenseRequset;
import com.mybudget.server.dto.expense.ExpenseResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.DebtMovementRepository;
import com.mybudget.server.repositories.ExpenseRepository;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final AccountService accountService;
    private final DebtMovementRepository debtMovementRepository;

    @Transactional
    public ExpenseResponse addExpense(ExpenseRequset expenseRequest) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = new Expense();
        expense.setAmount(expenseRequest.getAmount());
        expense.setCategory(expenseRequest.getCategory());
        expense.setUser(currentUser);
        expense.setImage(expenseRequest.getImage());

        String accountId = expenseRequest.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        expense.setAccount(account);
        accountService.updateTotalBalanaceWithExpense(account, expenseRequest.getAmount());

        return mapToExpenseResponse(expenseRepository.save(expense));
    }

    // this method is used with schedule
    @Transactional
    public ExpenseResponse addExpense(ExpenseRequset requset, User user) {
        Expense expense = new Expense();
        expense.setAmount(requset.getAmount());
        expense.setCategory(requset.getCategory());
        expense.setUser(user);

        String accountId = requset.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        expense.setAccount(account);
        accountService.updateTotalBalanaceWithExpense(account, requset.getAmount());

        return mapToExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(String expenseId, ExpenseRequset expenseRequest) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = findOwnedExpense(expenseId, currentUser);
        Account newAccount = findOwnedAccount(expenseRequest.getAccount(), currentUser);

        Account oldAccount = expense.getAccount();
        double oldAmount = expense.getAmount();
        double newAmount = expenseRequest.getAmount();
        boolean sameAccount = oldAccount != null && oldAccount.getId().equals(newAccount.getId());
        if (sameAccount) {
            accountService.applyCashFlow(newAccount, CashFlow.OUT, newAmount - oldAmount);
        } else {
            if (oldAccount != null) {
                accountService.revertCashFlow(oldAccount, CashFlow.OUT, oldAmount);
            }
            accountService.applyCashFlow(newAccount, CashFlow.OUT, newAmount);
        }
        try {
            expense.setAccount(newAccount);
            expense.setAmount(newAmount);
            expense.setCategory(expenseRequest.getCategory());
            expense.setImage(expenseRequest.getImage());
            if (expenseRequest.getCreatedAt() != null) {
                expense.setCreatedAt(expenseRequest.getCreatedAt());
            }
            return mapToExpenseResponse(expenseRepository.save(expense));
        } catch (RuntimeException e) {
            if (sameAccount) {
                accountService.applyCashFlow(newAccount, CashFlow.OUT, oldAmount - newAmount);
            } else {
                accountService.revertCashFlow(newAccount, CashFlow.OUT, newAmount);
                if (oldAccount != null) {
                    accountService.applyCashFlow(oldAccount, CashFlow.OUT, oldAmount);
                }
            }
            throw e;
        }
    }

    private void ensureNotDebtMirror(String recordId){
        if (debtMovementRepository.existsByMirroredTransactionId(recordId)){
            throw new ResourceNotFoundException(("This Expense was Created by debt movement, Edit or delete it from the debt"));
        }
    }
    private Expense findOwnedExpense(String expenseId, User currentUser) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (expense.getUser() == null || !expense.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Expense not found");
        }
        return expense;
    }

    private Account findOwnedAccount(Account requesstAccount, User currentUser) {
        if (requesstAccount == null || requesstAccount.getId() == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(requesstAccount.getId(), currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }

    @Transactional
    public String deleteExpense(String expenseId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = findOwnedExpense(expenseId, currentUser);
        ensureNotDebtMirror(expense.getId());
        accountService.updateTotalBalanceWithDeleteExpense(expense.getAccount(), expense.getAmount());
        expenseRepository.delete(expense);
        return "Expense deleted successfully";
    }

    public ExpenseResponse getExpenseById(String expenseId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found or access denied"));

        if (!expense.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Expense not found or access denied");
        } else {
            return mapToExpenseResponse(expense);
        }
    }

    public List<ExpenseResponse> getAllExpensesByUser() {
        User user = userUtils.getCurrentAuthenticatedUser();
        List<Expense> expenses = expenseRepository.findAllByUser(user);
        return expenses.stream().map(this::mapToExpenseResponse).toList();
    }

    public List<ExpenseResponse> getAllExpensesByAccount(Account account) {
        List<Expense> expenses = expenseRepository.findAllByAccount(account);
        return expenses.stream().map(this::mapToExpenseResponse).toList();
    }


    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        Map<String, String> image = expense.getImage();
        Map<String, String> imageUrl = new HashMap<>();
        if (image != null && image.get("url") != null) {
            imageUrl.put("url", image.get("url"));
            imageUrl.put("filename", image.get("filename"));
        } else {
            imageUrl.put("url", "");
            imageUrl.put("filename", "");
        }
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getUser(),
                expense.getAccount(),
                imageUrl
        );
    }
}
