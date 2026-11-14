package com.mybudget.server.services.statement;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.statement.StatementRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The numbers of an account statement for one currency and one period, from the account's
 * transaction rows and the balance it has now:
 *
 *   closing  = balance now − what happened after the period
 *   opening  = closing − what happened in the period
 *   rows     = the period, newest first, each with the balance right after it
 *
 * A cancelled row is listed but moves nothing (its balance is the one of the row before).
 * A transfer row already holds its fee on the same pocket (302 = 300 + 2), so one line.
 */
public final class StatementCalculator {

    private static final Set<String> INCOMING = Set.of("incomse", "in-transfer", "transfer-fee-in", "debt-in");

    private StatementCalculator() {
    }

    public record Result(double opening, double closing, double totalIn, double totalOut, List<StatementRow> rows) {
    }

    public static boolean isIncoming(Transaction t) {
        return INCOMING.contains(t.getType());
    }

    // what the row did to the balance (0 when cancelled)
    public static double counted(Transaction t) {
        if (t.isCancelled()) {
            return 0.0;
        }
        double amount = Math.abs(t.getAmount());
        return isIncoming(t) ? amount : -amount;
    }

    public static Result calculate(List<Transaction> transactions, String currency,
                                   LocalDate from, LocalDate to, double balanceNow,
                                   java.util.function.Function<Transaction, String> describe) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Transaction> inCurrency = transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> currency.equalsIgnoreCase(t.getCurrency()))
                .toList();

        double after = inCurrency.stream()
                .filter(t -> !t.getCreatedAt().isBefore(end))
                .mapToDouble(StatementCalculator::counted)
                .sum();
        double closing = round(balanceNow - after);

        List<Transaction> period = new ArrayList<>(inCurrency.stream()
                .filter(t -> !t.getCreatedAt().isBefore(start) && t.getCreatedAt().isBefore(end))
                .toList());
        // newest first; the same moment: the higher number first
        period.sort(Comparator.comparing(Transaction::getCreatedAt)
                .thenComparing(t -> t.getNumber() == null ? 0L : t.getNumber())
                .reversed());

        double running = closing;
        double totalIn = 0;
        double totalOut = 0;
        List<StatementRow> rows = new ArrayList<>();
        for (Transaction t : period) {
            double effect = counted(t);
            rows.add(new StatementRow(t.getCreatedAt(), t.getNumber(), t.getType(), describe.apply(t),
                    Math.abs(t.getAmount()), isIncoming(t), round(running), t.isCancelled(), t.getCancelReason()));
            running -= effect;
            if (effect > 0) totalIn += effect;
            if (effect < 0) totalOut -= effect;
        }
        return new Result(round(running), closing, round(totalIn), round(totalOut), rows);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}