package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllAccounts {
    private List<Account> accounts;
    private int totalAccounts;
    private String totalBalance;
}

