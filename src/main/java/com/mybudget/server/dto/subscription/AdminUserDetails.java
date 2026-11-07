package com.mybudget.server.dto.subscription;

import com.mybudget.server.modules.SubscriptionPayment;

import java.util.List;

// the admin user page: the user, the subscription and every recorded payment
public record AdminUserDetails(
        AdminUserSummary user,
        List<SubscriptionPayment> payments
) {
}
