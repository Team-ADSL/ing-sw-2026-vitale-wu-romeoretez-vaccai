package org.example.server.model.cards.buildings.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingBonusTest {

    private BuildingBonus bonus;

    @BeforeEach
    void setUp() {
        bonus = new BuildingBonus(2, 3, 2, 1, true, true, true, true, true);
    }

    @Test
    void constructor_setsExtraStars() {
        assertEquals(2, bonus.getExtraStars());
    }

    @Test
    void constructor_setsShamanMultiplier() {
        assertEquals(3, bonus.getShamanMulitiplierPP());
    }

    @Test
    void constructor_setsBuilderMultiplier() {
        assertEquals(2, bonus.getBuilderMultiplierPP());
    }

    @Test
    void constructor_setsSustenanceDiscount() {
        assertEquals(1, bonus.getSustenanceDiscount());
    }

    @Test
    void constructor_setsHuntEventBonus() {
        assertTrue(bonus.isHuntEventBonus());
    }

    @Test
    void constructor_setsExtraMove() {
        assertTrue(bonus.isExtraMove());
    }

    @Test
    void constructor_setsNoRitualLostPP() {
        assertTrue(bonus.isNoRitualLostPP());
    }

    @Test
    void constructor_setsArtistFood() {
        assertTrue(bonus.isArtistFood());
    }

    @Test
    void constructor_setsBonusFoodTile() {
        assertTrue(bonus.isBonusFoodTile());
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
        BuildingBonus b = new BuildingBonus(0, 1, 1, 0, false, false, false, false, false);
        b.setSustenanceDiscount(2);
        b.setSustenanceDiscount(3);
        assertEquals(5, b.getSustenanceDiscount());
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
