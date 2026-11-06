package com.mybudget.server.services.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// What one sync run did: returned by the admin endpoints and written to the log.
public class SyncReport {
    private static final int MAX_ERRORS = 20;

    private final String direction;
    private final Instant startedAt = Instant.now();
    private int collections;
    private long copied;
    private long deleted;
    private long failed;
    private boolean aborted;
    private final List<String> errors = new ArrayList<>();

    public SyncReport(String direction) {
        this.direction = direction;
    }

    void collectionDone() { collections++; }
    void copied() { copied++; }
    void deleted(long count) { deleted += count; }

    void failed(String message) {
        failed++;
        if (errors.size() < MAX_ERRORS) {
            errors.add(message);
        }
    }

    void abort(String message) {
        aborted = true;
        errors.add(message);
    }

    public String getDirection() { return direction; }
    public Instant getStartedAt() { return startedAt; }
    public int getCollections() { return collections; }
    public long getCopied() { return copied; }
    public long getDeleted() { return deleted; }
    public long getFailed() { return failed; }
    public boolean isAborted() { return aborted; }
    public List<String> getErrors() { return errors; }

    @Override
    public String toString() {
        return direction + ": " + (aborted ? "ABORTED" : "done")
                + ", collections=" + collections + ", copied=" + copied
                + ", deleted=" + deleted + ", failed=" + failed
                + (errors.isEmpty() ? "" : ", first error=" + errors.get(0));
    }
}