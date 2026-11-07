package com.mybudget.server.dto.subscription;

import com.mybudget.server.modules.enums.SubscriptionStatus;

import java.time.LocalDate;

// What the app needs to show the subscription state (banner, settings page)
public record SubscriptionInfo(
        SubscriptionStatus status,
        LocalDate endsAt,        // last day with full access, null when not set
        LocalDate graceEndsAt,   // last day before read only, null when not set
        Long daysLeft,           // until endsAt; negative after it; null when not set
        boolean canWrite
) {
}
