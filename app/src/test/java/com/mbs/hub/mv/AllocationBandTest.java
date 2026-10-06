package com.mbs.hub.mv;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** DEC-011 band thresholds (03 AC-006.2 edges). */
class AllocationBandTest {

    @Test void underSixty_isDarkGreen() {
        assertThat(AllocationBand.of(new BigDecimal("50")))
            .isEqualTo(AllocationBand.DARK_GREEN);
        assertThat(AllocationBand.of(new BigDecimal("59.99")))
            .isEqualTo(AllocationBand.DARK_GREEN);
    }

    @Test void sixtyToEightyFive_isLightGreen() {
        assertThat(AllocationBand.of(new BigDecimal("60")))
            .isEqualTo(AllocationBand.LIGHT_GREEN);
        assertThat(AllocationBand.of(new BigDecimal("70")))
            .isEqualTo(AllocationBand.LIGHT_GREEN);
        assertThat(AllocationBand.of(new BigDecimal("85")))
            .isEqualTo(AllocationBand.LIGHT_GREEN);
    }

    @Test void aboveEightyFiveToHundred_isYellow() {
        assertThat(AllocationBand.of(new BigDecimal("85.01")))
            .isEqualTo(AllocationBand.YELLOW);
        assertThat(AllocationBand.of(new BigDecimal("95")))
            .isEqualTo(AllocationBand.YELLOW);
        assertThat(AllocationBand.of(new BigDecimal("100")))
            .isEqualTo(AllocationBand.YELLOW);
    }

    @Test void aboveHundred_isRed() {
        assertThat(AllocationBand.of(new BigDecimal("100.01")))
            .isEqualTo(AllocationBand.RED);
        assertThat(AllocationBand.of(new BigDecimal("150")))
            .isEqualTo(AllocationBand.RED);
    }
}
