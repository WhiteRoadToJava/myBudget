package com.mybudget.server.dto.expense;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
public class ExpenseRequset {
    private  String name;
    private double amount;
    private String category;
    private Date createdAt;
    private User user;
    private Account account;
}
