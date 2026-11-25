package com.mybudget.server.services.currency;

import java.util.*;

public final class CurrencyRules {
    public static final List<String> ENABLED_AT_START = List.of("SEK", "EUR", "USD", "GBP", "AED", "SAR", "TRY", "JPY");
    public static final List<String> PAYMENT_AT_START = List.of("SYP", "USD", "EUR", "SEK");

    private CurrencyRules() {
    }


    public static String code(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("A currency is a 3-letter code such as SEK");
        }
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown currency: " + code);
        }
        return code;
    }



    public static List<String> order(List<String> current, List<String> requested){
        if (requested == null){
            throw new IllegalArgumentException("Give every currency of the list in the new order");
        }
        List<String> codes = requested
                .stream()
                .map(CurrencyRules::code)
                .toList();
        if (codes.size() != current.size() || !new HashSet<>(codes).equals(new HashSet<>(current))
                || new HashSet<>(codes).size() != codes.size()) {
            throw new IllegalArgumentException("Give every currency of the list in the new order");
        }
        return codes;
    }

}
