package com.mybudget.server.modules.enums;

public enum DebtDirection {
    OWED_BY_ME,
    OWED_TO_ME;

    public CashFlow cashFlowOf(DebtMovementType movementType) {
        boolean disbursement = movementType == DebtMovementType.DISBURSEMENT;
        boolean borrowed = this == OWED_BY_ME;
        return (disbursement == borrowed) ? CashFlow.IN : CashFlow.OUT;
    }
}
