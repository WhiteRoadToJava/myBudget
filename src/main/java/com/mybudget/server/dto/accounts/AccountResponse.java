package com.mybudget.server.dto.accounts;

import lombok.*;

import java.util.ArrayList;
@Getter
@Setter
@NoArgsConstructor
public class AccountResponse {
    private ArrayList allTransactions;
    private AccountInfo accountInfo;



    @Data
    public static class AccountInfo {
        private String name;
        private String currency;
        private Double balance;
        private String type;
    }
}