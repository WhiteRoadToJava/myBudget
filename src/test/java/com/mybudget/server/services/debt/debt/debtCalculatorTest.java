package com.mybudget.server.services.debt.debt;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private static Movement in(String date, String amount) {
        return new Movement(DebtMovementType.DISBURSEMENT, LocalDate.parse(date), new BigDecimal(amount));
    }

    private static Movement out(String date, String amount) {
        return new Movement(DebtMovementType.REPAYMENT, LocalDate.parse(date), new BigDecimal(amount));
    }

    // BigDecimal.equals also compares the scale (100.0 != 100.00), so compare the numeric value
    private static void assertMoney(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    @Test
    void withoutInterestTheDebtIsJustDisbursedMinusRepaid() {
        Result result = DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000"), out("2026-02-01", "300")), LocalDate.of(2026, 3, 1));

        assertMoney("700.00", result.principal());
        assertMoney("0.00", result.accruedInterest());
        assertMoney("700.00", result.totalOwed());
        assertMoney("1000.00", result.totalDisbursed());
        assertMoney("300.00", result.totalRepaid());
    }

    @Test
    void simpleInterestForExactlyOneYear() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("10"), InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000")), LocalDate.of(2027, 1, 1));

        assertMoney("1000.00", result.principal());
        assertMoney("100.00", result.accruedInterest());
        assertMoney("1100.00", result.totalOwed());
    }

    @Test
    void aRepaymentPaysTheInterestFirstThenThePrincipal() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("10"), InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000"), out("2027-01-01", "150")), LocalDate.of(2027, 1, 1));

        assertMoney("100.00", result.lines().get(1).interestPart());
        assertMoney("50.00", result.lines().get(1).principalPart());
        assertMoney("950.00", result.principal());
        assertMoney("0.00", result.accruedInterest());
        assertMoney("100.00", result.interestPaid());
        assertMoney("50.00", result.principalPaid());
    }

    @Test
    void partialYearRepaymentSplit() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("5"), InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000"), out("2026-07-01", "400")), LocalDate.of(2027, 1, 1));

        assertMoney("24.79", result.lines().get(1).interestPart());
        assertMoney("375.21", result.lines().get(1).principalPart());
        assertMoney("624.79", result.principal());
        assertMoney("15.75", result.accruedInterest());
        assertMoney("640.54", result.totalOwed());
    }

    @Test
    void monthlyCompoundingAddsTheInterestToThePrincipalEveryMonth() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("12"), InterestCompounding.MONTHLY,
                List.of(in("2026-01-01", "1000")), LocalDate.of(2026, 3, 1));

        // Jan: 10.19 -> 1010.19, Feb: 9.30 -> 1019.49 (both capitalized, the last one on the asOf day)
        assertMoney("1019.49", result.principal());
        assertMoney("0.00", result.accruedInterest());
    }

    @Test
    void yearlyCompoundingTwoYears() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("10"), InterestCompounding.YEARLY,
                List.of(in("2026-01-01", "1000")), LocalDate.of(2028, 1, 1));

        assertMoney("1210.00", result.totalOwed());
    }

    @Test
    void studentLoanWithMonthlyDisbursements() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("6"), InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000"), in("2026-02-01", "1000"), in("2026-03-01", "1000")),
                LocalDate.of(2026, 3, 31));

        assertMoney("3000.00", result.principal());
        assertMoney("29.10", result.accruedInterest());
        assertMoney("3029.10", result.totalOwed());
        assertMoney("3000.00", result.totalDisbursed());
    }

    @Test
    void theOrderOfTheInputDoesNotMatter() {
        List<Movement> one = List.of(in("2026-01-01", "1000"), out("2026-02-01", "200"), in("2026-03-01", "500"));
        List<Movement> other = List.of(in("2026-03-01", "500"), out("2026-02-01", "200"), in("2026-01-01", "1000"));

        Result first = DebtCalculator.calculate(START, new BigDecimal("7.5"), InterestCompounding.MONTHLY,
                one, LocalDate.of(2026, 6, 15));
        Result second = DebtCalculator.calculate(START, new BigDecimal("7.5"), InterestCompounding.MONTHLY,
                other, LocalDate.of(2026, 6, 15));

        assertEquals(0, first.totalOwed().compareTo(second.totalOwed()));
        assertEquals(0, first.principal().compareTo(second.principal()));
    }

    @Test
    void disbursementAndRepaymentOnTheSameDayWork() {
        Result result = DebtCalculator.calculate(START, new BigDecimal("5"), InterestCompounding.NONE,
                List.of(out("2026-01-01", "100"), in("2026-01-01", "100")), LocalDate.of(2026, 1, 1));

        assertMoney("0.00", result.totalOwed());
    }

    @Test
    void movementsAfterTheAsOfDateAreIgnored() {
        Result result = DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                List.of(in("2026-01-01", "1000"), out("2026-06-01", "500")), LocalDate.of(2026, 3, 1));

        assertMoney("1000.00", result.principal());
    }

    @Test
    void repayingMoreThanOwedIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                        List.of(in("2026-01-01", "100"), out("2026-01-01", "100.01")), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void repaymentBeforeAnyMoneyWasBorrowedIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                        List.of(out("2026-01-01", "10")), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void aMovementBeforeTheStartDateIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                        List.of(in("2025-12-31", "100")), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void zeroAmountAndNegativeRateAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                DebtCalculator.calculate(START, BigDecimal.ZERO, InterestCompounding.NONE,
                        List.of(in("2026-01-01", "0")), LocalDate.of(2026, 1, 1)));
        assertThrows(IllegalArgumentException.class, () ->
                DebtCalculator.calculate(START, new BigDecimal("-1"), InterestCompounding.NONE,
                        List.of(in("2026-01-01", "10")), LocalDate.of(2026, 1, 1)));
    }
}