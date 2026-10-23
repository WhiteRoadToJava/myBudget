package com.mybudget.server.dto.debt;


import com.mybudget.server.modules.enums.DebtMovementType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DebtMovementRequest {
    private DebtMovementType movementType;
    private double amount;
    private LocalDate date;
    private String accountId;
    private String description;
}
