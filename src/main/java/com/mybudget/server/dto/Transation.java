package com.mybudget.server.dto;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transation {
    private String id;
    private double amount;
    private String category;
    private Account account;
    private String type;
}
