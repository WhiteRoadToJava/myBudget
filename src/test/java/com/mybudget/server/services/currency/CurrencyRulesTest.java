package com.mybudget.server.services.currency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyRulesTest {

    @Test
    void aCodeIsTidied() {
        assertEquals("NOK", CurrencyRules.code(" nok "));
    }

    @Test
    void onlyIsoCurrenciesAreAccepted() {
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.code("XYZ"));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.code("SEKK"));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.code(""));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.code(null));
    }

    @Test
    void theNewOrderHoldsEveryCodeOnce() {
        List<String> current = List.of("SEK", "EUR", "USD");
        assertEquals(List.of("USD", "SEK", "EUR"), CurrencyRules.order(current, List.of("usd", "SEK", "eur")));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.order(current, List.of("SEK", "EUR")));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.order(current, List.of("SEK", "EUR", "EUR")));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.order(current, List.of("SEK", "EUR", "NOK")));
        assertThrows(IllegalArgumentException.class, () -> CurrencyRules.order(current, null));
    }

    @Test
    void theFirstListIsMadeOfRealCurrencies() {
        CurrencyRules.ENABLED_AT_START.forEach(CurrencyRules::code);
        CurrencyRules.PAYMENT_AT_START.forEach(CurrencyRules::code);
    }
}