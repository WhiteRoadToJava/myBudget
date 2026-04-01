package com.mybudget.server.dto.expense;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Setter
@Getter
@AllArgsConstructor
public class ExpenseResponse {
    private String id;
    private double amount;
    private String category;
    private LocalDateTime createdAt;
    private User user;
    private Account account;

}
