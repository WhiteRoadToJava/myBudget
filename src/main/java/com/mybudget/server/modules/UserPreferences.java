package com.mybudget.server.modules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPreferences {
    private String language;
    private String baseCurrency;
    private String firstDayOfWeek;
    private String defaultAccountId;
    // how amounts are shown: CODE (1 250 SEK) or SYMBOL (1 250 kr); null = CODE
    private String amountDisplay;
}
