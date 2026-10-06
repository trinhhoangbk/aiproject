package com.mbs.hub.mv;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** DEC-009 TPR bands. */
class TprBandTest {

    @Test void nullIsNone() {
        assertThat(TprBand.of(null)).isEqualTo(TprBand.NONE);
    }

    @Test void belowOrEqualPointSeven_isNone() {
        assertThat(TprBand.of(new BigDecimal("0.500"))).isEqualTo(TprBand.NONE);
        assertThat(TprBand.of(new BigDecimal("0.700"))).isEqualTo(TprBand.NONE);
    }

    @Test void abovePointSevenUpToOne_isYellow() {
        assertThat(TprBand.of(new BigDecimal("0.701"))).isEqualTo(TprBand.YELLOW);
        assertThat(TprBand.of(new BigDecimal("0.900"))).isEqualTo(TprBand.YELLOW);
        assertThat(TprBand.of(new BigDecimal("1.000"))).isEqualTo(TprBand.YELLOW);
    }

    @Test void aboveOne_isRed() {
        assertThat(TprBand.of(new BigDecimal("1.001"))).isEqualTo(TprBand.RED);
        assertThat(TprBand.of(new BigDecimal("1.500"))).isEqualTo(TprBand.RED);
    }
}
