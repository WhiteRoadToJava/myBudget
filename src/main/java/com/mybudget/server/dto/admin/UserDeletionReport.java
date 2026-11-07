package com.mybudget.server.dto.admin;

import java.util.Map;

// what was removed (documents per collection, receipt images) and the payments that were kept
public record UserDeletionReport(
        String username,
        Map<String, Long> deleted,
        int imagesDeleted,
        long subscriptionPaymentsKept
) {
}
