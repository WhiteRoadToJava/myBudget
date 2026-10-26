package com.mybudget.server.dto.debt;

import java.util.List;

public record DebtDetailsResponse(
        DebtResponse debt, List<DebtMovementResponse> movements
) {
}
