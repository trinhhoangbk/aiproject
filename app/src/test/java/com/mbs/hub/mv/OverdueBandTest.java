package com.mbs.hub.mv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** F-01 overdue severity bands. */
class OverdueBandTest {

    @Test void oneToThree() {
        assertThat(OverdueBand.of(1)).isEqualTo(OverdueBand.ONE_TO_3_DAYS);
        assertThat(OverdueBand.of(3)).isEqualTo(OverdueBand.ONE_TO_3_DAYS);
    }

    @Test void fourToSeven() {
        assertThat(OverdueBand.of(4)).isEqualTo(OverdueBand.FOUR_TO_7_DAYS);
        assertThat(OverdueBand.of(7)).isEqualTo(OverdueBand.FOUR_TO_7_DAYS);
    }

    @Test void moreThanAWeek() {
        assertThat(OverdueBand.of(8)).isEqualTo(OverdueBand.MORE_THAN_A_WEEK);
        assertThat(OverdueBand.of(30)).isEqualTo(OverdueBand.MORE_THAN_A_WEEK);
    }
}
