package org.adsl.server.model.cards.buildings.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingBonusTest {

    private BuildingBonus bonus;

    @BeforeEach
    void setUp() {
        bonus = new BuildingBonus();
    }

    @Test
    void testReset_setsExtraStarsToZero() {
        bonus.reset();
        assertEquals(0, bonus.getExtraStars());
    }

    @Test
    void testReset_setsShamanMultiplierToOne() {
        bonus.reset();
        assertEquals(1, bonus.getShamanMulitiplierPP());
    }

    @Test
    void testReset_setsBuilderMultiplierToOne() {
        bonus.reset();
        assertEquals(1, bonus.getBuilderMultiplierPP());
    }

    @Test
    void testReset_setsSustenanceDiscountToZero() {
        bonus.reset();
        assertEquals(0, bonus.getSustenanceDiscount());
    }

    @Test
    void testReset_clearsBooleanFlags() {
        bonus.reset();
        assertAll(
                () -> assertFalse(bonus.isHuntEventBonus()),
                () -> assertFalse(bonus.isExtraMove()),
                () -> assertFalse(bonus.isNoRitualLostPP()),
                () -> assertFalse(bonus.isArtistFood()),
                () -> assertFalse(bonus.isBonusFoodTile()),
                () -> assertFalse(bonus.isDoubleRitualPP())
        );
    }

    @Test
    void testSetSustenanceDiscount_accumulates() {
        bonus.setSustenanceDiscount(2);
        bonus.setSustenanceDiscount(3);
        assertEquals(5, bonus.getSustenanceDiscount());
    }

    @Test
    void testSetExtraStars_updatesValue() {
        bonus.setExtraStars(10);
        assertEquals(10, bonus.getExtraStars());
    }

    @Test
    void testSetDoubleRitualPP_updatesValue() {
        bonus.setDoubleRitualPP(true);
        assertTrue(bonus.isDoubleRitualPP());
    }
}
