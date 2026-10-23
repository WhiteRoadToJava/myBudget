package com.mybudget.server.services.debt.debt;

import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.modules.enums.DebtDirection;
import com.mybudget.server.modules.enums.DebtMovementType;
import com.mybudget.server.modules.enums.InterestCompounding;
import com.mybudget.server.services.debt.DebtCalculator;
import com.mybudget.server.services.debt.DebtCalculator.Movement;
import com.mybudget.server.services.debt.DebtCalculator.Result;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DebtDirectionTest {
    @Test
    void whenIBorrowedTheDisbursementBringsMoneyIn() {
        assertEquals(CashFlow.IN, DebtDirection.OWNED_BY_ME.cashFlowOf(DebtMovementType.DISBURSEMENT));
    }

    @Test
    void whenBorrowedTheRepaymentTakesMoneyout() {
        assertEquals(CashFlow.OUT, DebtDirection.OWNED_BY_ME.cashFlowOf(DebtMovementType.REPAYMENT));
    }

    @Test
    void whenILentTheDisbursementTakesMoneyOut() {
        assertEquals(CashFlow.OUT, DebtDirection.OWNED_TO_ME.cashFlowOf(DebtMovementType.DISBURSEMENT));
    }

    @Test
    void whenILentTheRepaymentBringsMoneyIn() {
        assertEquals(CashFlow.IN, DebtDirection.OWNED_TO_ME.cashFlowOf(DebtMovementType.REPAYMENT));
    }

    @Test
    void theRefOfAMovementComesBackInTheLines() {
        Movement first = new Movement("first", DebtMovementType.DISBURSEMENT, LocalDate.parse("2026-01-01"), new BigDecimal("100"));
        Movement second = new Movement("second", DebtMovementType.REPAYMENT, LocalDate.parse("2026-02-01"), new BigDecimal("40"));

        // given in the "wrong" order on purpose: the lines come back in date order
        Result result = DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                List.of(second, first), LocalDate.of(2026, 3, 1));

        assertEquals("first", result.lines().get(0).movement().ref());
        assertEquals("second", result.lines().get(1).movement().ref());
    }
}
