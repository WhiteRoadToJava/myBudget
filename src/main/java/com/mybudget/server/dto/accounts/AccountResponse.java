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
}