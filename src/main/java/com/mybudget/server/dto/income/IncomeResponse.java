package com.mybudget.server.dto.income;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;


@Getter @Setter
@AllArgsConstructor
public class IncomeResponse {
    private String id;
    private double amount;
    private String category;
    private User user;
    private Account account;
    private String type = "income";
    private LocalDateTime createAt;
    private Map<String, String> image;

}