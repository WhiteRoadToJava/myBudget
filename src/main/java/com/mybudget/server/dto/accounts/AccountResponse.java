package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.enums.AccountStatus;
import lombok.*;

import java.util.Set;

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
    private Set<AccountStatus> status;
    private String createdAt;
    // every currency of the account, with its opening balance and its balance now
    private java.util.List<com.mybudget.server.modules.CurrencyBalance> balances;
}