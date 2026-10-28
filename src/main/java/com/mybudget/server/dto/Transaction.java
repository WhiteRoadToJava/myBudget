package com.mybudget.server.dto;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String id;
    private double amount;
    private String category;
    private Account sourceAccount;
    private Account destinationAccount;
    private String type;
    private LocalDateTime createdAt;
    private String description;
    private Map<String, String> image;
    private String debtId;


    public Transaction(String id, double amount, String category, Account sourceAccount,
                       Account destinationAccount, String type, LocalDateTime createdAt,
                       String description, Map<String, String> image) {
        this(id, amount, category, sourceAccount, destinationAccount, type, createdAt, description, image, null);
    }
}