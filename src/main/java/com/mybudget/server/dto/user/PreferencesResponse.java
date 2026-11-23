package com.mybudget.server.dto.user;

public record PreferencesResponse(
        String language,
        String baseCurrency,
        String firstDayOfWeek,
        String defaultAccountId
) {
}
