package com.mybudget.server.modules;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One currency inside an account (a "pocket"): its opening balance and its balance now
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyBalance {
    private String currency;
    private Double openingBalance;
    private Double totalBalance;
}
