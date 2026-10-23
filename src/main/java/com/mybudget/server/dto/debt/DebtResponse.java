package com.mybudget.server.dto.debt;

import com.mybudget.server.modules.enums.DebtDirection;
import com.mybudget.server.modules.enums.InterestCompounding;

import java.time.LocalDate;

public record DebtResponse (
        String id,
        String name,
        String counterparty,
        DebtDirection direction,
        String currency,
        double annualRate,
        InterestCompounding compounding,
        LocalDate startDate,
        boolean countInflowsAsIncome,
        boolean countOutflowsAsExpense,
        String note,
        DebtSummeryResponse summary
){}
