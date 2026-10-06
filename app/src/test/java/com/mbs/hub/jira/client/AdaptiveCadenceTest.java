package com.mbs.hub.jira.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** TD-COND-04 adaptive cadence behavior. */
class AdaptiveCadenceTest {

    @Test
    void baselineAtStart() {
        assertThat(new AdaptiveCadence().current()).isEqualTo(AdaptiveCadence.BASELINE);
    }

    @Test
    void doublesOn429_cappedAtMax() {
        AdaptiveCadence c = new AdaptiveCadence();
        c.on429();                         // 2h
        assertThat(c.current()).isEqualTo(Duration.ofHours(2));
        c.on429(); c.on429(); c.on429();   // → 16h, capped to 6h
        assertThat(c.current()).isEqualTo(AdaptiveCadence.MAX_BACKOFF);
    }

    @Test
    void decaysOnSuccess_toBaseline() {
        AdaptiveCadence c = new AdaptiveCadence();
        c.on429(); c.on429();  // 4h
        c.onSuccess();         // → 2h
        assertThat(c.current()).isEqualTo(Duration.ofHours(2));
        c.onSuccess();         // → baseline
        assertThat(c.current()).isEqualTo(AdaptiveCadence.BASELINE);
        c.onSuccess();         // stays at baseline
        assertThat(c.current()).isEqualTo(AdaptiveCadence.BASELINE);
    }
}
