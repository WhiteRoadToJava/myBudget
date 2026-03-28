package com.mybudget.server.dto.accounts;

import lombok.*;

import java.util.ArrayList;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String name;
    private String currency;
    private String type;
    private Double balance;
    private Double totalBalance;
}