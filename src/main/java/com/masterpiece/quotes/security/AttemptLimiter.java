package com.masterpiece.quotes.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Fixed-window counter per key (client IP). Small and in-memory: there is one instance of this
// app, and losing the counters on restart is acceptable.
public class AttemptLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Window w = windows.get(key);
        return w != null && !w.expired(clock.instant()) && w.count >= maxAttempts;
    }

    public void record(String key) {
        Instant now = clock.instant();
        windows.compute(key, (k, w) -> (w == null || w.expired(now)) ? new Window(now.plus(window), 1) : w.increment());
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().expired(now));
        }
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private record Window(Instant expiresAt, int count) {
        boolean expired(Instant now) {
            return !now.isBefore(expiresAt);
        }

        Window increment() {
            return new Window(expiresAt, count + 1);
        }
    }
}
