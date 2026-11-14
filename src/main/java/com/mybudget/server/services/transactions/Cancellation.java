package com.mybudget.server.services.transactions;

import com.mybudget.server.exeptions.ConflictException;

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
}
