package com.mybudget.server.services.transactions;

import com.mybudget.server.exeptions.ConflictException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Cancellation {
    public static final int MAX_REASON = 200;

    private Cancellation() {
    }

    public static boolean isCancelled(Boolean cancelled) {
        return Boolean.TRUE.equals(cancelled);
    }

    public static String reason(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > MAX_REASON ? trimmed.substring(0, MAX_REASON) : trimmed;

    }

    public static void ensureActive(Boolean cancelled, String what) {
        if (isCancelled((cancelled))) {
            throw new ConflictException("This " + what + " is cancelled. Restore it first");
        }
    }


    public static void ensureCancelled(Boolean cancelled, String what){
        if (!isCancelled(cancelled)){
            throw new ConflictException("This " + what + " is not cancelled");
        }
    }


    //when it was cancelled: now , or the day the user chose, never in the future and never
    // before the transaction itseif the same day at an earlier hour moves to the transaction's time
    public static LocalDateTime date(LocalDateTime chosen, LocalDateTime createdAt){
        if (chosen == null){
            return LocalDateTime.now();
        }
        if (chosen.toLocalDate().isAfter(LocalDate.now())){
            throw new IllegalArgumentException("The cancellation date can not be in the future.");
        }
        if (createdAt != null && chosen.isBefore(createdAt)) {
            if (chosen.toLocalDate().isBefore(createdAt.toLocalDate())) {
                throw new IllegalArgumentException("hte cancellation date can not be before the transaction.");
            }
            return createdAt;
        }
        return chosen;
    }
}
