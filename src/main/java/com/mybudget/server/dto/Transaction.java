package com.mybudget.server.dto;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String id;
    private double amount;
    private String category;
    private Account account;
    private String type;
    private LocalDateTime createdAt;
}
