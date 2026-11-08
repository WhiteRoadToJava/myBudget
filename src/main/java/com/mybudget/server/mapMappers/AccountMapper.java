package com.mybudget.server.mapMappers;


import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.modules.*;
import com.mybudget.server.modules.enums.AccountStatus;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.services.TransferEffects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AccountMapper {

    public Transaction mapTransferToTransaction(Transfer transfer, Account account){
        String type = "";
        String category = "";
        Double amount = 0.0;
        LocalDateTime dateTime = transfer.getCreatedAt() != null ? transfer.getCreatedAt() : LocalDateTime.of(2011, 1,1,0,0);


        Map<String, String> imageUrl = transfer.getImage();
        Map<String, String> image = new HashMap<>();
        if (imageUrl != null && imageUrl.get("url") != null) {
            image.put("url", imageUrl.get("url"));
            image.put("filename", imageUrl.get("filename"));
        } else{
            image.put("url","");
            image.put("filename", "");
        }

        Account realSource = transfer.getSourceAccount();
        Account realDestination = transfer.getDestinationAccount();

        // the amount is what the transfer did to this account, fees included
        double net = TransferEffects.netFor(transfer, account.getId());
        if (isSameAccount(realSource, account)){
            category = "transfer";
            type = "out-transfer";
            amount = Math.abs(net);
        } else if (isSameAccount(realDestination, account)){
            category = "transfer";
            type = "in-transfer";
            amount = Math.abs(net);
        } else if (net != 0) {
            // only a fee was paid from or into this account
            category = "transfer";
            type = net > 0 ? "transfer-fee-in" : "transfer-fee-out";
            amount = Math.abs(net);
        } else {
            return null;
        }
        Account sourceAccount = realSource != null ? realSource : deletedAccount();
        Account destinationAccount = realDestination != null ? realDestination : deletedAccount();
        return  new Transaction(
                transfer.getId(),
                amount,
                category,
                sourceAccount,
                destinationAccount,
                type,
                dateTime,
                transfer.getDescription(),
                image
        );
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
                date
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
        return new Transaction(
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
        return new Transaction(
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
    }

    // Lists of all accounts: the transfer once (from the source, or the destination
    // when the source was deleted), plus one fee row for every other account a fee touched.
    public List<Transaction> mapTransferForUser(Transfer transfer){
        List<Transaction> rows = new ArrayList<>();
        Account main = transfer.getSourceAccount() != null ? transfer.getSourceAccount() : transfer.getDestinationAccount();
        if (main != null) {
            rows.add(mapTransferToTransaction(transfer, main));
        }
        for (TransferEffects.Effect effect : TransferEffects.of(transfer)) {
            Account feeAccount = effect.account();
            if (!isSameAccount(transfer.getSourceAccount(), feeAccount)
                    && !isSameAccount(transfer.getDestinationAccount(), feeAccount)) {
                rows.add(mapTransferToTransaction(transfer, feeAccount));
            }
        }
        rows.removeIf(Objects::isNull);
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
                debt.getId()
        );
    }

}
