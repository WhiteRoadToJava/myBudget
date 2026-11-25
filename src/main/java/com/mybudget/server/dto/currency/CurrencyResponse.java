package com.mybudget.server.dto.currency;

import com.mybudget.server.modules.SupportedCurrency;

public record CurrencyResponse(
        String code,
        boolean enabled,
        boolean payment,
        int position
) {
    public static CurrencyResponse of(SupportedCurrency currency){
        return new CurrencyResponse(currency.getCode(),
                currency.isEnabled(),
                currency.isPayment(),
                currency.getPosition());
    }
}
