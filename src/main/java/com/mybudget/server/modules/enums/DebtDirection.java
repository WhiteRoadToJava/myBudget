package com.mybudget.server.modules.enums;

public enum DebtDirection {
    OWNED_BY_ME,
    OWNED_TO_ME;

    public CashFlow cashFlowOf(DebtMovementType movementType) {
        boolean disbursement = movementType == DebtMovementType.DISBURSEMENT;
        boolean borrowed = this == OWNED_BY_ME;
        return (disbursement == borrowed) ? CashFlow.IN : CashFlow.OUT;
    }
}
