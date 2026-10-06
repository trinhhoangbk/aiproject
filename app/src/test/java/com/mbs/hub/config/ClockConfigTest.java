package com.mbs.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Verifies F-TD-01: the Hub clock reports {@code Asia/Saigon} regardless of
 * the host JVM default timezone. Test TZ is forced to {@code UTC} by Gradle
 * ({@code systemProperty("user.timezone","UTC")} in {@code build.gradle.kts}).
 */
class ClockConfigTest {

    @Test
    void clockBeanOverridesHostTimezone() {
        // Sanity: the test JVM is running with user.timezone=UTC
        assertThat(ZoneId.systemDefault().getId()).isEqualTo("UTC");

        Clock clock = new ClockConfig().hubClock();

        assertThat(clock.getZone()).isEqualTo(ClockConfig.HUB_ZONE);
        assertThat(clock.getZone().getId()).isEqualTo("Asia/Saigon");
    }
}
