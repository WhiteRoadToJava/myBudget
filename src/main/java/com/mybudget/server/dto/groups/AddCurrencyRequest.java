package com.mybudget.server.dto.groups;

import lombok.Data;

@Data
public class AddCurrencyRequest {
    private String currency;
    private Double openingBalance;
}
