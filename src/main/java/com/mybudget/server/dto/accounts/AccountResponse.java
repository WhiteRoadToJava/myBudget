package com.mybudget.server.dto.accounts;

import lombok.*;

import java.util.ArrayList;
@Getter
@Setter
@NoArgsConstructor
public class AccountResponse {
    private ArrayList allTransactions;
    @Data
    public static class accouteInfo {
        private String name;
        private String currency;
        private String type;
        private String category;
        private String subCategory;
        private String icon;
    }
}