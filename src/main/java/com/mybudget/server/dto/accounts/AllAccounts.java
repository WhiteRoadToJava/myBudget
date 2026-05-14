package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllAccounts {
    private List<Account> accounts;
    private int totalAccounts;
   private Map<String, Double> totalBalanceByCurrency = new HashMap<>();

}

