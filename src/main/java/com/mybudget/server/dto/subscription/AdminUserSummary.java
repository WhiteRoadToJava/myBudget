package com.mybudget.server.dto.subscription;

// one row of the admin user search
public record AdminUserSummary(
        String id,
        String username,
        String firstName,
        String lastName,
        String phone,
        boolean enabled,
        boolean admin,
        String createdAt,
        SubscriptionInfo subscription
) {
}
