package com.mybudget.server.dto.user;

import lombok.Data;

@Data
public class PreferencesRequest {
    private String language;
    private String baseCurrency;
    private String firstDayOfWeek;
    private String defaultAccountId;
}
