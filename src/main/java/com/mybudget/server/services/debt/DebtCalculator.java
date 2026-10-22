package com.mybudget.server.services.debt;

import com.mybudget.server.modules.enums.DebtMovementType;
import com.mybudget.server.modules.enums.InterestCompounding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure calculation of a debt: no database, no Spring, so it is easy to read and to test.
 *
 * Rules:
 *  - interest is charged per day on the outstanding principal: principal * rate% * days / 365
 *  - every interval between two events is rounded to 2 decimals (HALF_UP)
 *  - a REPAYMENT pays the accrued interest first, then the principal
 *  - with MONTHLY / YEARLY compounding the unpaid interest is added to the principal on every
 *    anniversary of the start date (that is what makes interest earn interest)
 *  - on the same day: capitalization first, then disbursements, then repayments
 */
public final class DebtCalculator {

    // 100 (percent -> fraction) * 365 (days in a year)
    private static final BigDecimal PERCENT_DAYS = new BigDecimal("36500");

    private DebtCalculator() {
    }

    public record Movement(DebtMovementType type, LocalDate date, BigDecimal amount) {
    }

    /** How one movement was split: for a repayment, how much went to interest and how much to principal. */
    public record Line(Movement movement, BigDecimal interestPart, BigDecimal principalPart) {
    }

    public record Result(
            BigDecimal principal,        // outstanding principal (includes capitalized interest)
            BigDecimal accruedInterest,  // interest that is not paid and not capitalized yet
            BigDecimal totalOwed,        // principal + accruedInterest
            BigDecimal totalDisbursed,
            BigDecimal totalRepaid,
            BigDecimal interestPaid,
            BigDecimal principalPaid,
            List<Line> lines) {
    }

    public static Result calculate(LocalDate startDate,
                                   BigDecimal annualRatePercent,
                                   InterestCompounding compounding,
                                   List<Movement> movements,
                                   LocalDate asOf) {
        if (startDate == null || asOf == null) {
            throw new IllegalArgumentException("startDate and asOf are required");
        }
        if (asOf.isBefore(startDate)) {
            throw new IllegalArgumentException("asOf is before the start date of the debt");
        }
        BigDecimal rate = annualRatePercent == null ? BigDecimal.ZERO : annualRatePercent;
        if (rate.signum() < 0) {
            throw new IllegalArgumentException("The interest rate can not be negative");
        }
        InterestCompounding mode = compounding == null ? InterestCompounding.NONE : compounding;

        List<Movement> sorted = new ArrayList<>();
        for (Movement movement : movements) {
            if (movement.amount() == null || movement.amount().signum() <= 0) {
                throw new IllegalArgumentException("Every amount must be greater than zero");
            }
            if (movement.date().isBefore(startDate)) {
                throw new IllegalArgumentException("A movement on " + movement.date()
                        + " is before the start date of the debt (" + startDate + ")");
            }
            if (!movement.date().isAfter(asOf)) { // movements after asOf have not happened yet
                sorted.add(new Movement(movement.type(), movement.date(), money(movement.amount())));
            }
        }
        sorted.sort(Comparator
                .comparing(Movement::date)
                .thenComparing(m -> m.type() == DebtMovementType.DISBURSEMENT ? 0 : 1));

        Ledger ledger = new Ledger(startDate, rate);
        int nextMovement = 0;
        int nextCapitalization = 1;

        while (true) {
            LocalDate movementDate = nextMovement < sorted.size() ? sorted.get(nextMovement).date() : null;
            LocalDate capitalizationDate = capitalizationDate(startDate, mode, nextCapitalization);
            if (capitalizationDate != null && capitalizationDate.isAfter(asOf)) {
                capitalizationDate = null;
            }
            if (movementDate == null && capitalizationDate == null) {
                break;
            }

            // when both fall on the same day the capitalization goes first
            if (capitalizationDate != null && (movementDate == null || !movementDate.isBefore(capitalizationDate))) {
                ledger.accrueUntil(capitalizationDate);
                ledger.capitalize();
                nextCapitalization++;
            } else {
                Movement movement = sorted.get(nextMovement++);
                ledger.accrueUntil(movement.date());
                ledger.apply(movement);
            }
        }
        ledger.accrueUntil(asOf);

        return new Result(
                ledger.principal,
                ledger.interest,
                ledger.principal.add(ledger.interest),
                ledger.totalDisbursed,
                ledger.totalRepaid,
                ledger.interestPaid,
                ledger.principalPaid,
                List.copyOf(ledger.lines));
    }

    // k-th anniversary of the start date, always counted from the start date so the day never drifts
    private static LocalDate capitalizationDate(LocalDate startDate, InterestCompounding mode, int number) {
        return switch (mode) {
            case NONE -> null;
            case MONTHLY -> startDate.plusMonths(number);
            case YEARLY -> startDate.plusYears(number);
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** The running state while we walk through the timeline. */
    private static final class Ledger {
        private final BigDecimal rate;
        private LocalDate cursor;

        private BigDecimal principal = money(BigDecimal.ZERO);
        private BigDecimal interest = money(BigDecimal.ZERO);
        private BigDecimal totalDisbursed = money(BigDecimal.ZERO);
        private BigDecimal totalRepaid = money(BigDecimal.ZERO);
        private BigDecimal interestPaid = money(BigDecimal.ZERO);
        private BigDecimal principalPaid = money(BigDecimal.ZERO);
        private final List<Line> lines = new ArrayList<>();

        private Ledger(LocalDate startDate, BigDecimal rate) {
            this.cursor = startDate;
            this.rate = rate;
        }

        private void accrueUntil(LocalDate date) {
            long days = ChronoUnit.DAYS.between(cursor, date);
            if (days > 0 && principal.signum() > 0 && rate.signum() > 0) {
                BigDecimal charged = principal
                        .multiply(rate)
                        .multiply(BigDecimal.valueOf(days))
                        .divide(PERCENT_DAYS, 2, RoundingMode.HALF_UP);
                interest = interest.add(charged);
            }
            cursor = date;
        }

        private void capitalize() {
            principal = principal.add(interest);
            interest = money(BigDecimal.ZERO);
        }

        private void apply(Movement movement) {
            BigDecimal amount = movement.amount();

            if (movement.type() == DebtMovementType.DISBURSEMENT) {
                principal = principal.add(amount);
                totalDisbursed = totalDisbursed.add(amount);
                lines.add(new Line(movement, money(BigDecimal.ZERO), amount));
                return;
            }

            BigDecimal owed = principal.add(interest);
            if (amount.compareTo(owed) > 0) {
                throw new IllegalArgumentException("The repayment of " + amount + " on " + movement.date()
                        + " is more than the " + owed + " owed on that day");
            }
            BigDecimal interestPart = amount.min(interest);
            BigDecimal principalPart = amount.subtract(interestPart);

            interest = interest.subtract(interestPart);
            principal = principal.subtract(principalPart);
            totalRepaid = totalRepaid.add(amount);
            interestPaid = interestPaid.add(interestPart);
            principalPaid = principalPaid.add(principalPart);
            lines.add(new Line(movement, interestPart, principalPart));
        }
    }
}