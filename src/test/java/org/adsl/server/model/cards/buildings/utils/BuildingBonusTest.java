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
    void reset_setsExtraStarsToZero() {
        bonus.reset();
        assertEquals(0, bonus.getExtraStars());
    }

    @Test
    void reset_setsShamanMultiplierToOne() {
        bonus.reset();
        assertEquals(1, bonus.getShamanMulitiplierPP());
    }

    @Test
    void reset_setsBuilderMultiplierToOne() {
        bonus.reset();
        assertEquals(1, bonus.getBuilderMultiplierPP());
    }

    @Test
    void reset_setsSustenanceDiscountToZero() {
        bonus.reset();
        assertEquals(0, bonus.getSustenanceDiscount());
    }

    @Test
    void reset_clearsBooleanFlags() {
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
    void setSustenanceDiscount_accumulates() {
        bonus.setSustenanceDiscount(2);
        bonus.setSustenanceDiscount(3);
        assertEquals(5, bonus.getSustenanceDiscount());
    }

    @Test
    void setExtraStars_updatesValue() {
        bonus.setExtraStars(10);
        assertEquals(10, bonus.getExtraStars());
    }

    @Test
    void setDoubleRitualPP_updatesValue() {
        bonus.setDoubleRitualPP(true);
        assertTrue(bonus.isDoubleRitualPP());
    }
}
