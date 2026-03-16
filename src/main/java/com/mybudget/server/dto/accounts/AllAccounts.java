package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.Account;
import lombok.Data;

import java.util.List;

@Data
public class AllAccounts {
    private List accounts;
    private AccountsInfo accountsInfo;





@Data
    public static class AccountsInfo {
    private int totalAccounts;
    private String totalBalance;

}

}
