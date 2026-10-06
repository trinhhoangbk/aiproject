package com.mbs.hub.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Produces a single {@link Clock} bean fixed to the business timezone
 * {@code Asia/Saigon} per DEC-002 and TD-003.
 *
 * <p>Every service that reasons about days, overdue, or TPR MUST use this
 * {@code Clock}; code must not call {@code LocalDate.now()} directly or
 * rely on the host JVM timezone. The accompanying unit test
 * {@code ClockConfigTest} verifies that this bean reports {@code Asia/Saigon}
 * even when the host is set to another timezone (finding F-TD-01).</p>
 */
@Configuration
public class ClockConfig {

    /** Business timezone — fixed per DEC-002. */
    public static final ZoneId HUB_ZONE = ZoneId.of("Asia/Saigon");

    @Bean
    public Clock hubClock() {
        return Clock.system(HUB_ZONE);
    }
}
