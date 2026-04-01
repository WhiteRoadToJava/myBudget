package com.mybudget.server.modules;


import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

import java.util.Date;

@Data
@NoArgsConstructor
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String name;
    private String description;
    private Double balance;
    private Double totalBalance;
    private String currency;
    private String type;
    @DBRef
    private User user;
    @CreatedDate
    private String createdAt;


    public Account(String name, String description, Double balance, Double totalBalance, String currency, String type,  User currentUser, String createdAt) {
        this.name = name;
        this.description = description;
        this.balance = balance;
        this.currency = currency;
        this.totalBalance = totalBalance;
        this.type = type;
        this.user = currentUser;
        this.createdAt = createdAt;

    }
}