package com.mybudget.server.modules;


import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@NoArgsConstructor
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String name;
    private String description;
    private Double balance;
    private String currency;
    private String type;
    @DBRef
    private User user;


    public Account(String name, String description, Double balance, String currency, String type, User currentUser) {
        this.name = name;
        this.description = description;
        this.balance = balance;
        this.currency = currency;
        this.type = type;
        this.user = currentUser;
    }
    
}