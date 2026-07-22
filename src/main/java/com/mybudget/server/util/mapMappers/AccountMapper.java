package com.mybudget.server.util.mapMappers;


import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AccountMapper {

    public Transaction mapTransferToTransaction(Transfer transfer, Account account){
        String type = "";
        Account sourceAccount = new Account();
        Account destinationAccount = new Account();
        String category = "";
        Double amount = 0.0;

        Map<String, String> imageUrl = transfer.getImage();
        Map<String, String> image = new HashMap<>();
        if (imageUrl != null && imageUrl.get("url") != null) {
            image.put("url", imageUrl.get("url"));
            image.put("filename", imageUrl.get("filename"));
        } else{
            image.put("url","");
            image.put("filename", "");
        }

        if(transfer.getSourceAccount().getId().equals(account.getId())) {
            category = "transfer";
            type= "out-transfer";
            sourceAccount = transfer.getSourceAccount();
            destinationAccount = transfer.getDestinationAccount();
            amount = transfer.getAmountSent();
        } else if (transfer.getDestinationAccount().getId().equals(account.getId())) {
            category = "transfer";
            type= "in-transfer";
            sourceAccount = transfer.getSourceAccount();
            destinationAccount = transfer.getDestinationAccount();
            amount = transfer.getAmountReceived();
        }
        return  new Transaction(
                transfer.getId(),
                amount,
                category,
                sourceAccount,
                destinationAccount,
                type,
                transfer.getCreatedAt(),
                transfer.getDescription(),
                image
        );
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
    public Transaction mapIToTransation (Incomse incomse){
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
                null,
                image
        );
    }
    public Transaction mapExpenseToTranaction(Expense expense) {
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
                null,
                image
        );
    }

    public Transaction mapTransferToTransaction(Transfer transfer){
        if(transfer.getSourceAccount() != null){
            Account sourceAccount = transfer.getSourceAccount();
            return mapTransferToTransaction(transfer, sourceAccount);
        }
        return  null;
    }

}
