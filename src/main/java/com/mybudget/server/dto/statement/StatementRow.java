package com.mybudget.server.dto.statement;

import java.time.LocalDateTime;

public record StatementRow(
        LocalDateTime date,
        Long number,
        String type,
        String description,
        double amount,
        boolean incoming,
        double balance,
        boolean cancelled,
        String cancelReason
) {
}
