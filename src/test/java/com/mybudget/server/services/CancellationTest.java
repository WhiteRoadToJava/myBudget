package com.mybudget.server.services;

import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.services.transactions.Cancellation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}