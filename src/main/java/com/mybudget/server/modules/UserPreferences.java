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
}
