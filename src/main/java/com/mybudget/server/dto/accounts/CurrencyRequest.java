package com.mybudget.server.dto.accounts;

import lombok.Data;

// a currency of an account: { "currency": "USD", "openingBalance": 50 }
@Data
public class CurrencyRequest {
    private String currency;
    private Double openingBalance;
}
