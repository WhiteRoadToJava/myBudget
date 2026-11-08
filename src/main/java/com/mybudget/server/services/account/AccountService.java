package com.mybudget.server.services.account;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.dto.accounts.UpdateAccountStatus;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private final ScheduleRepository scheduleRepository;
    private final MongoTemplate mongoTemplate;


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
        // the first currency of the account
        AccountBalances.pockets(account);
        AccountResponse accountResponse = accountMapper.mapToAccountResponse(accountRepository.save(account));
        return accountResponse;
    }

    public AccountResponse updateAccount(Account accouunt) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account existedAccount = accountRepository.findByIdAndUser(accouunt.getId(), currentUser);
        if (existedAccount == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        String primary = AccountBalances.primaryCurrency(existedAccount);
        // the records of a currency are in that currency: another currency is added, not switched to
        if (accouunt.getCurrency() != null && primary != null && !accouunt.getCurrency().equalsIgnoreCase(primary)) {
            throw new IllegalArgumentException("The currency of an account can not be changed. Add a currency instead");
        }
        existedAccount.setName(accouunt.getName());
        existedAccount.setDescription(accouunt.getDescription());
        existedAccount.setType(accouunt.getType());
        if (accouunt.getBalance() != null && primary != null) {
            AccountBalances.setOpening(existedAccount, primary, accouunt.getBalance());
        }
        return accountMapper.mapToAccountResponse(accountRepository.save(existedAccount));
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
        // schedules on a deleted account could never run again
        scheduleRepository.deleteAllBySourceAccountOrDestinationAccount(account, account);

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
            for (CurrencyBalance pocket : AccountBalances.pockets(account)) {
                totalBalanceByCurrency.merge(pocket.getCurrency(),
                        pocket.getTotalBalance() == null ? 0.0 : pocket.getTotalBalance(), Double::sum);
            }
        }

        AllAccounts allAccounts = new AllAccounts(accounts, totalAccounts, totalBalanceByCurrency);
        return allAccounts;
    }


    public AccountResponse updateAccountStatus(String accountId, UpdateAccountStatus status) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Account targedAccount = accountRepository.findByIdAndUser(accountId, currentUser);
        if (targedAccount == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (status == null || status.getStatus() == null || status.getStatus().isEmpty()) {
            throw new IllegalArgumentException("A status is required");
        }

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
        List<Transfer> transferList = transferRepository.findAllBySourceAccountOrDestinationAccountOrSendFeeAccountOrReceiveFeeAccount(
                account, account, account, account);
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
        return withDebtIds(transactions, currentUser);
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
                .flatMap(t -> accountMapper.mapTransferForUser(t).stream())
                .toList());
        transactions.addAll(mapDebtMovements(
                debtMovementRepository.findAllByUserAndMirroredTransactionIdIsNull(currentUser)
        ));

        return withDebtIds(transactions, currentUser);
    }

    public List<Transaction> getAllIncomeExpenseTransactions() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        List<Incomse> incomseList = incomseRepository.findAllByUser(currentUser);
        List<Expense> expenseList = expenseRepository.findAllByUser(currentUser);

        List<Transaction> transactionList = new ArrayList<>();
        transactionList.addAll(incomseList.stream().map(this::mapIToTransaction).toList());
        transactionList.addAll(expenseList.stream().map(this::mapExpenseToTransaction).toList());
        return withDebtIds(transactionList, currentUser);
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


    @Transactional
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
                .flatMap(t -> accountMapper.mapTransferForUser(t).stream())
                .toList());
        transactionList.addAll(mapDebtMovements(
                debtMovementRepository.findAllByUserAndDateBetweenAndMirroredTransactionIdIsNull(
                        currentUser, Range.rightOpen(from, to))));
        return withDebtIds(transactionList, currentUser);
    }


    private Transaction mapTransferToTransaction(Transfer transfer, Account account) {
        return accountMapper.mapTransferToTransaction(transfer, account);
    }

    private Transaction mapExpenseToTransaction(Expense expense) {
        return accountMapper.mapExpenseToTransaction(expense);
    }

    private Transaction mapIToTransaction(Incomse incomse) {
        return accountMapper.mapIToTransaction(incomse);
    }



    // money in or out of one currency of an account
    public void applyCashFlow(Account account, String currency, CashFlow cashFlow, double amount) {
        if (account == null) {
            return; // a record of a deleted account moves no balance
        }
        AccountBalances.change(account, currency, cashFlow == CashFlow.IN ? amount : -amount);
        accountRepository.save(account);
    }

    public void revertCashFlow(Account account, String currency, CashFlow cashFlow, double amount) {
        applyCashFlow(account, currency, cashFlow == CashFlow.IN ? CashFlow.OUT : CashFlow.IN, amount);
    }

    // still used by transfers and debts until they move to currencies (next phases): the first currency
    public void applyCashFlow(Account account, CashFlow cashFlow, double amount) {
        applyCashFlow(account, account == null ? null : AccountBalances.primaryCurrency(account), cashFlow, amount);
    }

    public void revertCashFlow(Account account, CashFlow cashFlow, double amount) {
        applyCashFlow(account, cashFlow == CashFlow.IN ? CashFlow.OUT : CashFlow.IN, amount);
    }

    // ---------- currencies of an account ----------

    public AccountResponse addCurrency(String accountId, String currency, Double openingBalance) {
        Account account = findOwnedAccount(accountId);
        AccountBalances.add(account, currency, openingBalance == null ? 0.0 : openingBalance);
        return accountMapper.mapToAccountResponse(accountRepository.save(account));
    }

    public AccountResponse setOpeningBalance(String accountId, String currency, Double openingBalance) {
        Account account = findOwnedAccount(accountId);
        String code = AccountBalances.resolve(account, currency);
        AccountBalances.setOpening(account, code, openingBalance == null ? 0.0 : openingBalance);
        return accountMapper.mapToAccountResponse(accountRepository.save(account));
    }

    // only a currency nothing was recorded in, and never the last one
    public AccountResponse removeCurrency(String accountId, String currency) {
        Account account = findOwnedAccount(accountId);
        String code = AccountBalances.resolve(account, currency);
        List<CurrencyBalance> pockets = AccountBalances.pockets(account);
        if (pockets.size() <= 1) {
            throw new ConflictException("An account needs at least one currency");
        }
        if (isCurrencyUsed(account, code)) {
            throw new ConflictException("This currency has transactions. It can only be removed when it has none");
        }
        pockets.removeIf(p -> code.equalsIgnoreCase(p.getCurrency()));
        // the old single-currency fields follow the first remaining currency
        if (code.equalsIgnoreCase(account.getCurrency())) {
            CurrencyBalance first = pockets.get(0);
            account.setCurrency(first.getCurrency());
            account.setBalance(first.getOpeningBalance());
            account.setTotalBalance(first.getTotalBalance());
        }
        return accountMapper.mapToAccountResponse(accountRepository.save(account));
    }

    // any income, expense, transfer (or fee), schedule or debt movement in that currency of the account;
    // a record without currency (from before the migration) belongs to the first currency
    private boolean isCurrencyUsed(Account account, String currency) {
        boolean primary = currency.equalsIgnoreCase(AccountBalances.primaryCurrency(account));
        Criteria inCurrency = primary
                ? new Criteria().orOperator(Criteria.where("currency").is(currency), Criteria.where("currency").is(null))
                : Criteria.where("currency").is(currency);
        java.util.function.BiFunction<String, String, Criteria> side = (accountField, currencyField) -> new Criteria().andOperator(
                Criteria.where(accountField).is(account),
                primary ? new Criteria().orOperator(Criteria.where(currencyField).is(currency), Criteria.where(currencyField).is(null))
                        : Criteria.where(currencyField).is(currency));

        Query records = new Query(new Criteria().andOperator(Criteria.where("account").is(account), inCurrency));
        if (mongoTemplate.exists(records, Incomse.class) || mongoTemplate.exists(records, Expense.class)
                || mongoTemplate.exists(records, DebtMovement.class)) {
            return true;
        }
        Query transfers = new Query(new Criteria().orOperator(
                side.apply("sourceAccount", "sourceCurrency"), side.apply("destinationAccount", "destinationCurrency"),
                side.apply("sendFeeAccount", "sendFeeCurrency"), side.apply("receiveFeeAccount", "receiveFeeCurrency")));
        Query schedules = new Query(new Criteria().orOperator(
                side.apply("sourceAccount", "sourceCurrency"), side.apply("destinationAccount", "destinationCurrency")));
        return mongoTemplate.exists(transfers, Transfer.class) || mongoTemplate.exists(schedules, Schedule.class);
    }

    private Account findOwnedAccount(String accountId) {
        Account account = accountRepository.findByIdAndUser(accountId, userUtils.getCurrentAuthenticatedUser());
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }


    // Income and expense records created by a debt movement get the id of their debt,
    // so the frontend can open the debt instead of offering edit/delete (which answer 409).
    private List<Transaction> withDebtIds(List<Transaction> transactions, User user) {
        Map<String, String> debtIdByRecord = new HashMap<>();
        for (DebtMovement movement : debtMovementRepository.findAllByUserAndMirroredTransactionIdIsNotNull(user)) {
            if (movement.getDebt() != null) {
                debtIdByRecord.put(movement.getMirroredTransactionId(), movement.getDebt().getId());
            }
        }
        if (debtIdByRecord.isEmpty()) {
            return transactions;
        }
        for (Transaction transaction : transactions) {
            if (transaction.getDebtId() == null && debtIdByRecord.containsKey(transaction.getId())) {
                transaction.setDebtId(debtIdByRecord.get(transaction.getId()));
            }
        }
        return transactions;
    }

    private List<Transaction> mapDebtMovements(List<DebtMovement> movements){
        return movements.stream()
                .map(accountMapper::mapDebtMovementToTransaction)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
