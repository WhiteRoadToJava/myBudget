package com.mybudget.server.dto.debt;

import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.modules.enums.DebtMovementType;

import java.time.LocalDate;

public record DebtMovementResponse(
        String id,
        DebtMovementType movementType,
        CashFlow cashFlow,
        double amount,
        LocalDate date,
        String accountId,
        String accountName,
        String description,
        double interestPart,
        double principalPart,
        boolean countedAsTransaction
        ) {
}
