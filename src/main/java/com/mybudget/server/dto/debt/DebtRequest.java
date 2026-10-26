package com.mybudget.server.dto.debt;


import com.mybudget.server.modules.enums.DebtDirection;
import com.mybudget.server.modules.enums.InterestCompounding;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DebtRequest {
    private String name;
    private String counterparty;
    private DebtDirection direction;
    private String currency;
    private double annualRate;
    private InterestCompounding compounding;
    private LocalDate startDate;
    private boolean countInflowsAsIncome;
    private boolean countOutflowsAsExpense;
    private String note;
}
