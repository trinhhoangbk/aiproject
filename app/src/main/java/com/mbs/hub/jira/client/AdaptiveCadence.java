package com.mbs.hub.jira.client;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * TD-COND-04 adaptive reconciler cadence: when Jira returns 429, double the
 * next tick's gap (bounded). On a run of successes, decay back toward baseline.
 * Thread-safe; one shared instance is enough for the single scheduler.
 */
@Component
public class AdaptiveCadence {

    public static final Duration BASELINE   = Duration.ofHours(1);
    public static final Duration MAX_BACKOFF = Duration.ofHours(6);

    private final AtomicReference<Duration> current = new AtomicReference<>(BASELINE);

    public Duration current() { return current.get(); }

    /** Call after a tick observed a 429. */
    public void on429() {
        current.updateAndGet(prev -> {
            Duration doubled = prev.multipliedBy(2);
            return doubled.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : doubled;
        });
    }

    /** Call after a successful tick (no 429, no network error). */
    public void onSuccess() {
        current.updateAndGet(prev -> prev.compareTo(BASELINE) <= 0 ? BASELINE
                : prev.dividedBy(2).compareTo(BASELINE) < 0 ? BASELINE
                : prev.dividedBy(2));
    }
}
