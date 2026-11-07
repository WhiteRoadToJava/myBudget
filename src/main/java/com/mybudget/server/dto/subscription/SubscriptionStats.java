package com.mybudget.server.dto.subscription;

// numbers for the admin overview
public record SubscriptionStats(
        long users,
        long trial,
        long active,
        long expiringIn7Days,
        long expired,
        long notSet
) {
}
