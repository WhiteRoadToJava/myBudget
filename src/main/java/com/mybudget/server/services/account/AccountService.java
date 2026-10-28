package com.mybudget.server.services.account;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.dto.accounts.UpdateAccountStatus;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.*;
import com.mybudget.server.modules.enums.AccountStatus;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.repositories.*;
import com.mybudget.server.util.UserUtils;
import com.mybudget.server.mapMappers.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserUtils userUtils;
    private final AccountRepository accountRepository;
    private final IncomseRepository incomseRepository;
    private final ExpenseRepository expenseRepository;
    private final TransferRepository transferRepository;
    private final AccountMapper accountMapper;
    private final DebtMovementRepository debtMovementRepository;


    public AccountResponse createAccount(AccountRequest accountRequest) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        if (findAccountByNameAndUser(accountRequest.getName(), currentUser)) {
            throw new ResourceNotFoundException("Account already exists");
        }
        Date createdAt = new Date();
        double openingBalance = accountRequest.getBalance() != null ? accountRequest.getBalance() : 0.0;

        Account account = new Account(
                accountRequest.getName(),
                accountRequest.getDescription(),
                openingBalance,
                openingBalance,
                accountRequest.getCurrency(),
                accountRequest.getType(),
                currentUser,
                createdAt.toString()
        );
        AccountResponse accountResponse = accountMapper.mapToAccountResponse(accountRepository.save(account));
        return accountResponse;
    }

    public AccountResponse updateAccount(Account accouunt) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account existedAccount = accountRepository.findByIdAndUser(accouunt.getId(), currentUser);
        if (existedAccount == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        existedAccount.setName(accouunt.getName());
        existedAccount.setDescription(accouunt.getDescription());
        existedAccount.setCurrency(accouunt.getCurrency());
        existedAccount.setType(accouunt.getType());

        double oldBalance = existedAccount.getBalance() != null ? existedAccount.getBalance() : 0.0;
        double newBalance = accouunt.getBalance() != null ? accouunt.getBalance() : 0.0;
        double oldTotalBalance = existedAccount.getTotalBalance();
        existedAccount.setTotalBalance(calculateNewTotal(oldBalance, newBalance, oldTotalBalance));
        existedAccount.setBalance(newBalance);
        return accountMapper.mapToAccountResponse(accountRepository.save(existedAccount));
    }

    private Double calculateNewTotal(Double oldBalance, Double newBalance, Double currentTotal) {

        return currentTotal + (newBalance - oldBalance);
    }

    public AccountResponse updateTotalBalanceWithReceivedTransfer(Account account, Double amount) {
        String accountId = account.getId();
        Account updatedAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Double totalBalance = account.getTotalBalance() + amount;
        updatedAccount.setTotalBalance(totalBalance);

        return accountMapper.mapToAccountResponse(accountRepository.save(updatedAccount));
    }

    public AccountResponse updateTotalBalanceWithSemdTransfer(Account account, Double amount) {
        String accountId = account.getId();
        Account updatedAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Double totalBalance = account.getTotalBalance() - amount;
        updatedAccount.setTotalBalance(totalBalance);

        return accountMapper.mapToAccountResponse(accountRepository.save(updatedAccount));
    }

    @Transactional
    public void deleteAccount(String accountId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (debtMovementRepository.existsByAccount(account)){
            throw new ConflictException("This account has debt movements. Delete or move them before deleting the account");
        }
        incomseRepository.deleteAllByAccount(account);
        expenseRepository.deleteAllByAccount(account);

        accountRepository.delete(account);
    }


    public AllAccounts getAllAccounts() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        List<Account> accounts = accountRepository.findAllByUser(currentUser);
        int totalAccounts = accounts.size();

        // ✅ currency → total balance
        Map<String, Double> totalBalanceByCurrency = new HashMap<>();

        for (Account account : accounts) {

            if (account.getStatus() == null) {
                account.setStatus(Collections.singleton(AccountStatus.ACTIVE));
            }

            // ✅ Group and sum by currency
            totalBalanceByCurrency.merge(
                    account.getCurrency(),
                    account.getTotalBalance(),
                    Double::sum
            );
        }

        AllAccounts allAccounts = new AllAccounts(accounts, totalAccounts, totalBalanceByCurrency);
        return allAccounts;
    }


    public AccountResponse updateAccountStatus(String accountId, UpdateAccountStatus status) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account targedAccount = accountRepository.findByIdAndUser(accountId, currentUser);

        targedAccount.setStatus(status.getStatus());

        accountRepository.save(targedAccount);
        return mapToAccountResponse(targedAccount);
    }


    public List<Transaction> getAllAccountTransactions(Account requestAccount) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        if (requestAccount == null || requestAccount.getId() == null){
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(requestAccount.getId(), currentUser);
        if (account == null){
            throw  new ResourceNotFoundException("Account not found");
        }
        List<Incomse> incomseList = incomseRepository.findAllByAccount(account);
        List<Expense> expenseList = expenseRepository.findAllByAccount(account);
        List<Transfer> transferList = transferRepository.findAllBySourceAccountOrDestinationAccount(account, account);
        List<Transaction> transactions = new ArrayList<>();
        transactions.addAll(incomseList.stream().map(this::mapIToTransaction).toList());
        transactions.addAll(expenseList.stream().map(this::mapExpenseToTransaction).toList());
        transactions.addAll(transferList.stream()
                .map(t -> mapTransferToTransaction(t, requestAccount))
                .filter(java.util.Objects::nonNull)
                .toList());
        transactions.addAll(mapDebtMovements(
                debtMovementRepository.findAllByAccountAndMirroredTransactionIdIsNull(account)
        ));
        return transactions;
    }

    public List<Transaction> getUserAllTransaction() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        List<Incomse> incomses = incomseRepository.findAllByUser(currentUser);
        List<Expense> expenses = expenseRepository.findAllByUser(currentUser);
        List<Transfer> transfers = transferRepository.findAllByUser(currentUser.getId());

        List<Transaction> transactions = new ArrayList<>();
        transactions.addAll(incomses.stream().map(this::mapIToTransaction).toList());
        transactions.addAll(expenses.stream().map(this::mapExpenseToTransaction).toList());
        transactions.addAll(transfers.stream()
                .map(this::mapTransferToTransaction)
                .filter(java.util.Objects::nonNull)
                .toList());
        transactions.addAll(mapDebtMovements(
                debtMovementRepository.findAllByUserAndMirroredTransactionIdIsNull(currentUser)
        ));

        return transactions;
    }

    public List<Transaction> getAllIncomeExpenseTransactions() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        List<Incomse> incomseList = incomseRepository.findAllByUser(currentUser);
        List<Expense> expenseList = expenseRepository.findAllByUser(currentUser);

        List<Transaction> transactionList = new ArrayList<>();
        transactionList.addAll(incomseList.stream().map(this::mapIToTransaction).toList());
        transactionList.addAll(expenseList.stream().map(this::mapExpenseToTransaction).toList());
        return transactionList;
    }


    private boolean findAccountByNameAndUser(String accountName, User currentUser) {
        return accountRepository.findByNameAndUser(accountName, currentUser).isPresent();
    }

    public AccountResponse getAccountById(String accountId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return mapToAccountResponse(account);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return accountMapper.mapToAccountResponse(account);
    }


    public Account updateTotalBalanaceWithExpense(Account account, double amount) {
        account.setTotalBalance(account.getTotalBalance() - amount);
        return accountRepository.save(account);
    }

    public void updateTotalBalanceWithIncome(Account account, double amount) {
        if (account != null) {
            if (account.getBalance() == null) {
                throw new ResourceNotFoundException("Balance is null");
            }
            account.setTotalBalance(account.getTotalBalance() + amount);
            accountRepository.save(account);
        }
    }

    @Transactional
    public void updateTotalBalanceWithDeleteIncome(Account account, Double amount) {
        if (account != null) {
            account.setTotalBalance(account.getTotalBalance() - amount);
            accountRepository.save(account);
        }
    }

    @Transactional
    public void updateTotalBalanceWithDeleteExpense(Account account, Double amount) {
        if (account != null) {
            account.setTotalBalance(account.getTotalBalance() + amount);
            accountRepository.save(account);
        }
    }


    @Transactional
    public String updateAccountsWithUpdateTransfer(Account accountSend, Account accountDestination, Transfer transfer, TransferRequest request) {
        if (accountSend != null) {
            Double oldAccountSendTotal = accountSend.getTotalBalance();
            Double newAccountSentTotal = oldAccountSendTotal + transfer.getAmountSent() - request.getAmountSent();
            accountSend.setTotalBalance(newAccountSentTotal);
            accountRepository.save(accountSend);

        }
        if (accountDestination != null) {
            Double oldAccountDestinationTotal = accountDestination.getTotalBalance();
            Double newAccountDestinationTotal = oldAccountDestinationTotal - transfer.getAmountReceived() + request.getAmountReceived();
            accountDestination.setTotalBalance(newAccountDestinationTotal);
            accountRepository.save(accountDestination);
        }
        return "success";
    }

    @Transactional
    public String updateAccountsTotalBalanceWithDeleteTransfer(Transfer transfer) {
        String sourceAccountId = "";
        if (transfer.getSourceAccount() != null) {
            sourceAccountId = transfer.getSourceAccount().getId();
            accountRepository.findById(sourceAccountId).ifPresent(sourceAccount -> {
                Double newBalance = sourceAccount.getTotalBalance() + transfer.getAmountSent();
                sourceAccount.setTotalBalance(newBalance);
                accountRepository.save(sourceAccount);
            });
        }
        String destinationAccountId = "";
        if (transfer.getDestinationAccount() != null) {
            destinationAccountId = transfer.getDestinationAccount().getId();
            accountRepository.findById(destinationAccountId).ifPresent(destinationAccount -> {
                Double newBalance = destinationAccount.getTotalBalance() - transfer.getAmountReceived();
                destinationAccount.setTotalBalance(newBalance);
                accountRepository.save(destinationAccount);
            });
        }
        return "success";
    }


    public List<Transaction> getTransactionsBetweenTwoDates(String fromDate, String toDate) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        LocalDateTime date1 = from.atStartOfDay();
        LocalDateTime date2 = to.atStartOfDay();
        List<Incomse> incomseList = incomseRepository.findByUserAndCreatedAtBetween(currentUser, date1, date2);
        List<Expense> expenseList = expenseRepository.findByUserAndCreatedAtBetween(currentUser, date1, date2);
        List<Transfer> transferList = transferRepository.findByUserAndCreatedAtBetween(currentUser, date1, date2);

        List<Transaction> transactionList = new ArrayList<>();
        transactionList.addAll(incomseList.stream().map(this::mapIToTransaction).toList());
        transactionList.addAll(expenseList.stream().map(this::mapExpenseToTransaction).toList());
        transactionList.addAll(transferList.stream()
                .map(this::mapTransferToTransaction)
                .filter(java.util.Objects::nonNull)
                .toList());
        transactionList.addAll(mapDebtMovements(
                debtMovementRepository.findAllByUserAndDateBetweenAndMirroredTransactionIdIsNull(
                        currentUser, Range.rightOpen(from, to))));
        return transactionList;
    }


    private Transaction mapTransferToTransaction(Transfer transfer, Account account) {
        return accountMapper.mapTransferToTransaction(transfer, account);
    }

    private Transaction mapExpenseToTransaction(Expense expense) {
        return accountMapper.mapExpenseToTranaction(expense);
    }

    private Transaction mapIToTransaction(Incomse incomse) {
        return accountMapper.mapIToTransation(incomse);
    }

    private Transaction mapTransferToTransaction(Transfer transfer) {
        return accountMapper.mapTransferToTransaction(transfer);
    }


    public void applyCashFlow(Account account, CashFlow cashFlow, double amount) {
        double current = account.getTotalBalance() == null ? 0.0 : account.getTotalBalance();
        double change = cashFlow == CashFlow.IN ? amount : -amount;
        account.setTotalBalance(roundToCents(current + change));
        accountRepository.save(account);
    }

    public void revertCashFlow(Account account, CashFlow cashFlow, double amount) {
        applyCashFlow(account, cashFlow == CashFlow.IN ? CashFlow.OUT : CashFlow.IN, amount);
    }

    private double roundToCents(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<Transaction> mapDebtMovements(List<DebtMovement> movements){
        return movements.stream()
                .map(accountMapper::mapDebtMovementToTransaction)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
