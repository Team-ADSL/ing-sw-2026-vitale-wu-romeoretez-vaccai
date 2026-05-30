package org.adsl.client.view.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutMathTest {

    @Test
    void cardWidth_fitsWithinMaxWhenAmpleSpace() {
        assertEquals(95.0, LayoutMath.cardWidth(2000, 6, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_shrinksToFitButNotBelowMin() {
        assertEquals(48.0, LayoutMath.cardWidth(100, 6, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_dividesSpaceMinusGaps() {
        assertEquals(95.0, LayoutMath.cardWidth(430, 4, 10, 48, 95), 0.001);
        assertEquals(60.0, LayoutMath.cardWidth(270, 4, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_zeroCardsReturnsMax() {
        assertEquals(95.0, LayoutMath.cardWidth(500, 0, 10, 48, 95), 0.001);
    }

    @Test
    void perPage_countsCardsThatFit() {
        assertEquals(4, LayoutMath.perPage(300, 60, 10));
    }

    @Test
    void perPage_atLeastOne() {
        assertEquals(1, LayoutMath.perPage(10, 60, 10));
    }

    @Test
    void fitScale_oneWhenContentFits() {
        assertEquals(1.0, LayoutMath.fitScale(800, 600, 1000, 800, 0.4), 0.001);
    }

    @Test
    void fitScale_shrinksToFitTheTighterAxis() {
        assertEquals(0.5, LayoutMath.fitScale(1000, 600, 500, 600, 0.4), 0.001);
    }

    @Test
    void fitScale_neverBelowFloor() {
        assertEquals(0.4, LayoutMath.fitScale(2000, 2000, 200, 200, 0.4), 0.001);
    }

    @Test
    void fitScale_safeOnZeroContent() {
        assertEquals(1.0, LayoutMath.fitScale(0, 0, 500, 500, 0.4), 0.001);
    }
}
