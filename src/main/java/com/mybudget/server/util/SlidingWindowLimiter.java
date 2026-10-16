package com.mybudget.server.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowLimiter {

    private static final long MAX_WINDOW_MS = 60L * 60 * 1000;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();


    public long tryAcquire(String key, int maxRequests, long windowMs, long nowMs){
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque());
        long retryAfterSeconds = 0;

        synchronized (queue) {
            while (!queue.isEmpty() && nowMs - queue.peekFirst() >= windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= maxRequests) {
                long waitMs = windowMs - (nowMs - queue.peekFirst());
                retryAfterSeconds = Math.max(1, (waitMs + 999) / 1000);
            }else {
                queue.addLast(nowMs);
            }
        }
        if (hits.size() > CLEANUP_THRESHOLD) {
            cleanup(nowMs);
        }
        return retryAfterSeconds;
    }

    private void cleanup(long nowMs) {
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || nowMs - last >= MAX_WINDOW_MS;
            }
        });
    }
}
