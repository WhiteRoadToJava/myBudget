package com.mybudget.server.services;

import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.services.transactions.Cancellation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CancellationTest {

    @Test
    void aBlankReasonIsNoReason() {
        assertNull(Cancellation.reason(null));
        assertNull(Cancellation.reason("   "));
        assertEquals("came back", Cancellation.reason("  came back "));
        assertEquals(Cancellation.MAX_REASON, Cancellation.reason("x".repeat(500)).length());
    }

    @Test
    void onlyAnActiveTransactionCanBeCancelledOrEdited() {
        assertDoesNotThrow(() -> Cancellation.ensureActive(null, "income"));
        assertDoesNotThrow(() -> Cancellation.ensureActive(false, "income"));
        assertThrows(ConflictException.class, () -> Cancellation.ensureActive(true, "income"));
    }

    @Test
    void onlyACancelledTransactionCanBeRestored() {
        assertDoesNotThrow(() -> Cancellation.ensureCancelled(true, "transfer"));
        assertThrows(ConflictException.class, () -> Cancellation.ensureCancelled(null, "transfer"));
    }



    @Test
    void theCancellationDateIsNowWhenNoneIsChosen() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime at = Cancellation.date(null, LocalDateTime.parse("2026-05-05T10:00:00"));
        assertTrue(!at.isBefore(before));
    }

    @Test
    void aChosenCancellationDateIsKeptBetweenTheTransactionAndToday() {
        LocalDateTime created = LocalDateTime.parse("2026-05-05T10:00:00");
        LocalDateTime chosen = LocalDateTime.parse("2026-06-20T09:00:00");
        assertEquals(chosen, Cancellation.date(chosen, created));
    }

    @Test
    void theSameDayAtAnEarlierHourMovesToTheTransactionsTime() {
        LocalDateTime created = LocalDateTime.parse("2026-05-05T15:00:00");
        assertEquals(created, Cancellation.date(LocalDateTime.parse("2026-05-05T09:00:00"), created));
    }

    @Test
    void aCancellationDateBeforeTheTransactionOrInTheFutureIsRefused() {
        LocalDateTime created = LocalDateTime.parse("2026-05-05T10:00:00");
        assertThrows(IllegalArgumentException.class,
                () -> Cancellation.date(LocalDateTime.parse("2026-05-04T23:00:00"), created));
        assertThrows(IllegalArgumentException.class,
                () -> Cancellation.date(LocalDateTime.now().plusDays(1), created));
    }
}