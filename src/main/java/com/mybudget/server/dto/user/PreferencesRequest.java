package com.mybudget.server.dto.user;

import lombok.Data;

@Data
public class PreferencesRequest {
    private String language;
    private String baseCurrency;
    private String firstDayOfWeek;
    private String defaultAccountId;
    // how amounts are shown: CODE (1 250 SEK) or SYMBOL (1 250 kr); null = CODE
    private String amountDisplay;

}
