package com.mybudget.server.mapMappers;

import com.mybudget.server.dto.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellationRowsTest {

    private static Transaction row(String id, boolean cancelled) {
        Transaction t = new Transaction(id, 40, "food", null, null, "expense",
                LocalDateTime.parse("2026-05-05T12:00:00"), "lunch", null);
        t.setCurrency("SEK");
        t.setNumber(7L);
        if (cancelled) {
            t.setCancelled(true);
            t.setCancelledAt(LocalDateTime.parse("2026-06-20T09:00:00"));
            t.setCancelReason("returned");
        }
        return t;
    }

    @Test
    void aCancelledRowGetsAReversalOnTheDayItWasCancelled() {
        List<Transaction> rows = CancellationRows.withReversals(List.of(row("a", false), row("b", true)));

        assertEquals(3, rows.size());
        Transaction reversal = rows.get(2);
        assertTrue(reversal.isReversal());
        assertEquals("b", reversal.getId());
        assertEquals(LocalDateTime.parse("2026-06-20T09:00:00"), reversal.getCreatedAt());
        assertEquals(7L, reversal.getNumber());
        assertEquals("SEK", reversal.getCurrency());
        assertEquals("returned", reversal.getCancelReason());
        assertFalse(rows.get(1).isReversal());   // the original is kept as it was
    }

    @Test
    void activeRowsGetNoReversal() {
        assertEquals(1, CancellationRows.withReversals(List.of(row("a", false))).size());
    }
}