package com.mybudget.server.services.statement;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.statement.StatementRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementCalculatorTest {

    private static Transaction row(String type, double amount, String currency, String when, long number) {
        Transaction t = new Transaction(null, amount, null, null, null, type, LocalDateTime.parse(when), null, null);
        t.setCurrency(currency);
        t.setNumber(number);
        return t;
    }

    private final LocalDate from = LocalDate.parse("2026-11-05");
    private final LocalDate to = LocalDate.parse("2026-11-05");

    @Test
    void theBalanceAfterEachRowNewestFirstWithTheOpeningBalanceUnder() {
        // like the ledger: -22,616.82 before, then -302 and -5,030 that day, then +100 the day after
        List<Transaction> rows = List.of(
                row("out-transfer", 302, "USD", "2026-11-05T09:00:00", 1),
                row("out-transfer", 5030, "USD", "2026-11-05T10:00:00", 2),
                row("incomse", 100, "USD", "2026-11-06T10:00:00", 3),
                row("incomse", 999, "SEK", "2026-11-05T11:00:00", 4));   // another currency: not here
        double now = -22616.82 - 302 - 5030 + 100;

        StatementCalculator.Result r = StatementCalculator.calculate(rows, "USD", from, to, now, t -> "");

        assertEquals(-27948.82, r.closing());
        assertEquals(-22616.82, r.opening());
        assertEquals(2, r.rows().size());
        StatementRow newest = r.rows().get(0);
        assertEquals(2L, newest.number());
        assertEquals(-27948.82, newest.balance());
        assertEquals(-22918.82, r.rows().get(1).balance());
        assertEquals(5332.0, r.totalOut());
    }


    @Test
    void aCancelledRowCountsOnItsDayAndItsReversalOnTheDayItWasCancelled() {
        // an expense of 40 on the 5th, cancelled on the 6th: the 5th still shows it
        Transaction expense = row("expense", 40, "SEK", "2026-11-05T12:00:00", 7);
        expense.setCancelled(true);
        Transaction reversal = row("expense", 40, "SEK", "2026-11-06T09:00:00", 7);
        reversal.setCancelled(true);
        reversal.setReversal(true);
        List<Transaction> rows = List.of(expense, reversal);

        StatementCalculator.Result day5 = StatementCalculator.calculate(rows, "SEK", from, to, 100, t -> "");
        assertEquals(1, day5.rows().size());
        assertEquals(100.0, day5.opening());
        assertEquals(60.0, day5.closing());
        assertEquals(40.0, day5.totalOut());

        StatementCalculator.Result both = StatementCalculator.calculate(rows, "SEK", from,
                LocalDate.parse("2026-11-06"), 100, t -> "");
        assertEquals(2, both.rows().size());
        assertTrue(both.rows().get(0).reversal());
        assertTrue(both.rows().get(0).incoming());
        assertEquals(100.0, both.opening());
        assertEquals(100.0, both.closing());
        assertEquals(0.0, both.totalOut());   // the reversal lowers what went out
        assertEquals(0.0, both.totalIn());
    }
}