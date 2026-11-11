package com.mybudget.server.mapMappers;


import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.modules.*;
import com.mybudget.server.modules.enums.AccountStatus;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.services.transactions.TransferEffects;
import com.mybudget.server.services.account.AccountBalances;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AccountMapper {

    // One row per pocket of this account that the transfer touches. Usually one; an exchange
    // inside the account (USD -> SYP) gives two: what left one currency, what came into the other.
    public List<Transaction> mapTransferForAccount(Transfer transfer, Account account){
        List<Transaction> rows = new ArrayList<>();
        for (TransferEffects.Effect effect : TransferEffects.of(transfer)) {
            if (isSameAccount(effect.account(), account)) {
                rows.add(rowFor(transfer, effect));
            }
        }
        return rows;
    }

    // a row for one pocket: the source pocket is "out-transfer", the destination pocket
    // "in-transfer", any other pocket only paid or got a fee
    private Transaction rowFor(Transfer transfer, TransferEffects.Effect effect) {
        String type;
        if (effect.isPocket(transfer.getSourceAccount(), TransferEffects.sourceCurrency(transfer))) {
            type = "out-transfer";
        } else if (effect.isPocket(transfer.getDestinationAccount(), TransferEffects.destinationCurrency(transfer))) {
            type = "in-transfer";
        } else {
            type = effect.amount() > 0 ? "transfer-fee-in" : "transfer-fee-out";
        }

        Map<String, String> imageUrl = transfer.getImage();
        Map<String, String> image = new HashMap<>();
        image.put("url", imageUrl != null && imageUrl.get("url") != null ? imageUrl.get("url") : "");
        image.put("filename", imageUrl != null && imageUrl.get("filename") != null ? imageUrl.get("filename") : "");
        LocalDateTime dateTime = transfer.getCreatedAt() != null ? transfer.getCreatedAt() : LocalDateTime.of(2011, 1, 1, 0, 0);

        Account sourceAccount = transfer.getSourceAccount() != null ? transfer.getSourceAccount() : deletedAccount();
        Account destinationAccount = transfer.getDestinationAccount() != null ? transfer.getDestinationAccount() : deletedAccount();
        Transaction row = new Transaction(
                transfer.getId(),
                Math.abs(effect.amount()),   // what the transfer did to this pocket, fees included
                "transfer",
                sourceAccount,
                destinationAccount,
                type,
                dateTime,
                transfer.getDescription(),
                image
        );
        row.setCurrency(effect.currency());
        markCancelled(row, transfer.getCancelled(), transfer.getCancelledAt(), transfer.getCancelReason() );
        row.setNumber(transfer.getNumber());
        return row;
    }

    private boolean isSameAccount(Account candidate, Account account){
        return candidate != null && candidate.getId() != null
                && candidate.getId().equals(account.getId());
    }

    private Account deletedAccount(){
        Account placedHolder = new Account();
        placedHolder.setName("Deleted account");
        return placedHolder;
    }

    private void markCancelled(Transaction row, Boolean cancelled, LocalDateTime at, String reason){
        if (Boolean.TRUE.equals(cancelled)){
            row.setCancelled(true);
            row.setCancelledAt(at);
            row.setCancelReason(reason);
        }
    }


    // a record's currency; one from before the migration is in its account's first currency
    private String currencyOf(String recordCurrency, Account account) {
        if (recordCurrency != null || account == null) {
            return recordCurrency;
        }
        return AccountBalances.primaryCurrency(account);
    }

    public AccountResponse mapToAccountResponse(Account account) {
        String date = account.getCreatedAt();
        if(date == null) date = "2010-10-10";
        Set<AccountStatus> status = account.getStatus();
        if(account.getStatus() == null || account.getStatus().isEmpty()){
            status = Collections.singleton(AccountStatus.ACTIVE);
        }
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getCurrency(),
                account.getType(),
                account.getBalance(),
                account.getTotalBalance(),
                status,
                date,
                AccountBalances.pockets(account)
        );
    }
    public Transaction mapIToTransaction(Incomse incomse){
        String type = "type";
        Map<String, String> imageUrl = incomse.getImage();
        Map<String, String> image = new HashMap<>();
        if (imageUrl != null && imageUrl.get("url") != null) {
            image.put("url", imageUrl.get("url"));
            image.put("filename", imageUrl.get("filename"));
        } else{
            image.put("url","");
            image.put("filename", "");
        }
        if(incomse.getType() == null){
            type= "incomse";
        }else type = incomse.getType();
        Transaction row = new Transaction(
                incomse.getId(),
                incomse.getAmount(),
                incomse.getCategory(),
                incomse.getAccount(),
                null,
                type,
                incomse.getCreatedAt(),
                incomse.getDescription(),
                image
        );
        row.setCurrency(currencyOf(incomse.getCurrency(), incomse.getAccount()));
        markCancelled(row, incomse.getCancelled(), incomse.getCancelledAt(), incomse.getCancelReason());
        row.setNumber(incomse.getNumber());
        return row;
    }
    public Transaction mapExpenseToTransaction(Expense expense) {
        String type;
        Map<String, String> imageUrl = expense.getImage();
        Map<String, String> image = new HashMap<>();
        if (imageUrl != null && imageUrl.get("url") != null) {
            image.put("url", imageUrl.get("url"));
            image.put("filename", imageUrl.get("filename"));
        } else{
            image.put("url","");
            image.put("filename", "");
        }
        if (expense.getType() == null) {
            type = "expense";
        } else type = expense.getType();
        LocalDateTime date = expense.getCreatedAt();
        Transaction row = new Transaction(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getAccount(),
                null,
                type,
                date,
                expense.getDescription(),
                image
        );
        row.setCurrency(currencyOf(expense.getCurrency(), expense.getAccount()));
        markCancelled(row, expense.getCancelled(), expense.getCancelledAt() ,expense.getCancelReason());
        row.setNumber(expense.getNumber());
        return row;
    }

    // Lists of all accounts: the transfer once (from its source pocket, or the destination
    // when the source account was deleted), plus one row for every other pocket a fee touched.
    // The destination pocket is not listed separately, so nothing is counted twice.
    public List<Transaction> mapTransferForUser(Transfer transfer){
        List<Transaction> rows = new ArrayList<>();
        String sourceCurrency = TransferEffects.sourceCurrency(transfer);
        String destinationCurrency = TransferEffects.destinationCurrency(transfer);
        boolean sourceGone = transfer.getSourceAccount() == null;
        for (TransferEffects.Effect effect : TransferEffects.of(transfer)) {
            boolean isSource = effect.isPocket(transfer.getSourceAccount(), sourceCurrency);
            boolean isDestination = effect.isPocket(transfer.getDestinationAccount(), destinationCurrency);
            if (isSource || (isDestination && sourceGone) || (!isSource && !isDestination)) {
                rows.add(rowFor(transfer, effect));
            }
        }
        return rows;
    }

    public Transaction mapDebtMovementToTransaction(DebtMovement movement){
        Debt debt = movement.getDebt();
        if (debt == null || debt.getDirection() == null || movement.getDate() == null){
            return null;
        }
        CashFlow cashFlow = debt.getDirection().cashFlowOf(movement.getMovementType());
        String type = cashFlow == CashFlow.IN ? "debt-in" : "debt-out";
        Account account = movement.getAccount() != null ? movement.getAccount() : deletedAccount();

        LocalDateTime when = movement.getCreatedAt() != null
                && movement.getCreatedAt().toLocalDate().equals(movement.getDate())
                ? movement.getCreatedAt()
                : movement.getDate().atTime(LocalTime.NOON);
        String description = movement.getDescription() != null && !movement.getDescription().isBlank()
                ? movement.getDescription()
                : debt.getName();

        Map<String, String> image = new HashMap<>();
        Map<String, String> imageUrl = movement.getImage();
        if (imageUrl != null && imageUrl.get("url") != null){
            image.put("url", imageUrl.get("url"));
            image.put("filename", imageUrl.get("filename"));
        } else {
            image.put("url", "");
            image.put("filename", "");
        }
        return new Transaction(
                movement.getId(),
                movement.getAmount(),
                "Debt",
                account,
                null,
                type,
                when,
                description,
                image,
                debt.getId(),
                debt.getCurrency(),
                false,
                null,
                null,
                movement.getNumber()
        );
    }

}
